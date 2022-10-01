package com.joojn.mixins;

import com.joojn.json.JsonLib;
import com.joojn.json.elements.JsonElement;
import com.joojn.json.elements.JsonObject;
import com.joojn.mixins.annotation.MixinMethod;
import com.joojn.mixins.annotation.MixinTarget;
import com.joojn.mixins.annotation.Shadow;
import com.joojn.mixins.element.*;
import com.joojn.mixins.exception.MixinException;
import com.joojn.mixins.transformer.CustomClassLoader;
import com.joojn.mixins.transformer.MixinTransformer;
import com.joojn.mixins.util.ReflectionHelper;
import javafx.util.Pair;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MixinMain {

    // FAKE RETURN VALUES - can't be final

    // Be aware that if return type is (Integer, Character, Long...)
    // you have to pass FAKE_OBJECT_RETURN not primitive type

    public static Object   FAKE_OBJECT_RETURN   =    null   ;
    public static boolean  FAKE_BOOLEAN_RETURN  =    false  ;
    public static int      FAKE_INT_RETURN      =    0      ;
    public static float    FAKE_FLOAT_RETURN    =    0F     ;
    public static double   FAKE_DOUBLE_RETURN   =    0D     ;
    public static long     FAKE_LONG_RETURN     =    0L     ;
    public static short    FAKE_SHORT_RETURN    =    0      ;
    public static char     FAKE_CHAR_RETURN     =    0      ;

    public static boolean debug = false;

    private MixinMain(){}

    private static PrintStream errPrintStream = System.err;
    private static PrintStream outPrintStream = System.out;

    public static void setErrorPrintStream(PrintStream printStream)
    {
        if(printStream == null) return;

        errPrintStream = printStream;
    }

    public static void setOutPrintStream(PrintStream printStream)
    {
        if(printStream == null) return;

        outPrintStream = printStream;
    }

    private static final HashMap<String, Pair<Class<?>, byte[]>> registeredClasses = new HashMap<>();

    public static void loadMixins(Instrumentation inst) throws ClassNotFoundException, IOException, NullPointerException
    {
        InputStream stream = ClassLoader.getSystemResourceAsStream("mixin-config.json");
        JsonObject object = JsonLib.parseString(String.join("\n", IOUtils.readLines(stream, StandardCharsets.UTF_8))).getAsJsonObject();

        log("Found mixins config file: " + object);

        for(JsonElement element : object.get("mixins").getAsJsonArray())
        {
            String className = element.getAsString();

            Pair<Class<?>, byte[]> classData = CustomClassLoader.INSTANCE.loadClassAndData(className);

            if(!classData.getKey().isAnnotationPresent(MixinTarget.class))
            {
                err("Class is not annotated with MixinTarget! " + classData.getClass().getName());
                continue;
            }

            registeredClasses.put(
                    classData.getKey().getAnnotation(MixinTarget.class).className().replace(".", "/"),
                    classData
            );
        }

        inst.addTransformer(new MixinTransformer(), true);
    }

    public static Pair<Class<?>, byte[]> getByName(String className)
    {
        return registeredClasses.get(className);
    }

    public static byte[] transformClass(Pair<Class<?>, byte[]> originalClassBuffer, byte[] classfileBuffer) throws MixinException
    {
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        Class<?> originalClass = originalClassBuffer.getKey();
        ClassReader currentClassReader = new ClassReader(originalClassBuffer.getValue());
        ClassNode currentClassNode = new ClassNode();
        currentClassReader.accept(currentClassNode, 0);

        // add shadow & create new elements
        final Set<ShadowElement> shadows = new HashSet<>();

        for(Field field : originalClass.getDeclaredFields())
        {
            if(field.isAnnotationPresent(Shadow.class))
            {
                shadows.add(new ShadowField(field, cn));
            }
            else // MixinNew
            {
                FieldNode fieldNode = NewField.findField(currentClassNode, field);

                cn.fields.add(fieldNode);
            }
        }

        for(Method method : originalClass.getDeclaredMethods())
        {
            if(method.isAnnotationPresent(Shadow.class))
            {
                shadows.add(new ShadowMethod(method, cn));
            }
            else if(!method.isAnnotationPresent(MixinMethod.class)) // mixin new
            {
                MethodNode methodNode = NewMethod.findMethod(currentClassNode, method, "Error while trying to create new method '%s'");

                cn.methods.add(methodNode);
            }
        }

        // second loop because all shadow etc. has to be added first
        for(Method method : originalClass.getDeclaredMethods())
        {
            if(method.isAnnotationPresent(MixinMethod.class))
            {
                MethodNode methodNode = NewMethod.findMethod(currentClassNode, method, "Error while trying to find method '%s'");
                MethodNode targetNode = MethodElement.findMethod(cn, method, method.getAnnotation(MixinMethod.class));

                log(
                        "Transforming method '%s.%s%s' from '%s'\n",
                        cn.name, targetNode.name, targetNode.desc,
                        currentClassNode.name
                );
;
                transformMethod(targetNode, methodNode, method.getAnnotation(MixinMethod.class));
                replaceShadows(targetNode, shadows);
            }
        }

        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);

        return cw.toByteArray();
    }

    private static void replaceShadows(MethodNode targetNode, Set<ShadowElement> shadows)
    {
        for(AbstractInsnNode node : targetNode.instructions)
        {
            if(node instanceof MethodInsnNode || node instanceof FieldInsnNode)
            {
                filterShadow(node, shadows);
            }
        }

        removeFakeReturn(targetNode.instructions);
    }

    private static void removeFakeReturn(InsnList instructions)
    {
        boolean found = false;
        AbstractInsnNode prevNode = null;

        for(int i = instructions.size() - 1; i >= 0 ; i--)
        {
            AbstractInsnNode node = instructions.get(i);

            // Opcodes.IRETURN == 172 && Opcodes.ARETURN == 176
            if(node.getOpcode() >= Opcodes.IRETURN && node.getOpcode() <= Opcodes.ARETURN)
            {
                found = true;
            }
            else if(found && node instanceof FieldInsnNode && node.getOpcode() == Opcodes.GETSTATIC)
            {
                FieldInsnNode field = (FieldInsnNode) node;

                if(
                        field.name.startsWith("FAKE")
                                && MixinMain.class.getName().replace(".", "/").equals(field.owner)
                )
                {
                    instructions.remove(prevNode);
                    instructions.remove(node);
                }
            }

            prevNode = node;
        }
    }

    private static void filterShadow(AbstractInsnNode node, Set<ShadowElement> shadows)
    {
        for(ShadowElement shadow : shadows)
        {
            if(
                    shadow.originalName()
                            .equals(ReflectionHelper.getField(node, "name"))
                    && shadow.getDesc()
                            .equals(ReflectionHelper.getField(node, "desc"))
            )
            {
                ReflectionHelper.setField(node, "name", shadow.getName());
                ReflectionHelper.setField(node, "desc", shadow.getDesc());
                ReflectionHelper.setField(node, "owner", shadow.getOwner());

                if(shadow.getOpcode() != -1)
                {
                    try
                    {
                        ReflectionHelper.setField(
                                AbstractInsnNode.class.getDeclaredField("opcode"),
                                node,
                                shadow.getOpcode()
                        );
                    }
                    catch (NoSuchFieldException e)
                    {
                        e.printStackTrace();
                    }
                }

                break;
            }
        }
    }

    private static void transformMethod(MethodNode targetNode, MethodNode methodNode, MixinMethod annotation)
    {
        if(annotation.override())
        {
            targetNode.instructions = methodNode.instructions; // replace instructions
            targetNode.exceptions = methodNode.exceptions; // replace exceptions
        }
        else
        {
            targetNode.exceptions.addAll(methodNode.exceptions); // add exceptions

            InsnList list = new InsnList();
            int line = annotation.atLine();
            int orig = line;

            int methodNodeSize = getReturnNodeIndex(targetNode.instructions);

            // example -1 => size = 10 => 9th index
            if(line < 0) line = methodNodeSize - line;

            // for 0 -> 9
            for(int i = 0; i < methodNodeSize ; i++)
            {
                if(orig < 0) list.add(targetNode.instructions.get(i));

                if(i == line)
                {
                    // remove RETURN
                    int size = getReturnNodeIndex(methodNode.instructions);

                    for(int j = 0; j < size ; j++)
                    {
                        list.add(methodNode.instructions.get(j));
                    }
                }

                if(orig >= 0) list.add(targetNode.instructions.get(i));
            }

            targetNode.instructions = list;
        }
    }

    private static int getReturnNodeIndex(InsnList list)
    {
        for( int i = list.size() - 1 ; i >= 0 ; i-- )
        {
            AbstractInsnNode n = list.get(i);
            if(n.getOpcode() == Opcodes.RETURN)
            {
                return i;
            }
        }

        return list.size();
    }

    public static void err(Throwable e)
    {
        e.printStackTrace(errPrintStream);
    }

    public static void err(String e)
    {
        errPrintStream.println(e);
    }

    public static void log(String info, Object... args)
    {
        outPrintStream.printf(info + "\n", args);
    }
}
