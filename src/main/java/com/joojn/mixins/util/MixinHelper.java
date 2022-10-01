package com.joojn.mixins.util;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class MixinHelper {

    public static boolean argsEquals(Type[] types, String desc)
    {
        String args = desc.substring(1, desc.lastIndexOf(")"));
        String args2 = Arrays.stream(types).map(Type::getDescriptor).collect(Collectors.joining());

        return args.equals(args2);
    }

    public static boolean methodEqualsWithoutReturnType(MethodNode node, Method method)
    {
        return node.name.equals(method.getName())
                && argsEquals(Type.getArgumentTypes(method), node.desc);
    }

    public static boolean methodEquals(MethodNode node, Method method)
    {
        return node.name.equals(method.getName())
                && Type.getMethodDescriptor(method).equals(node.desc);
    }

    public static boolean fieldEquals(FieldNode node, Field field)
    {
        return node.name.equals(field.getName())
                && Type.getType(field.getType()).getDescriptor().equals(node.desc);
    }

    public static boolean methodEquals(MethodInsnNode node, Method method)
    {
        return node.name.equals(method.getName())
                && Type.getType(method).getDescriptor().equals(node.desc)
                && method.getDeclaringClass().getName().replace(".", "/").equals(node.owner);
    }

    public static boolean fieldEquals(FieldInsnNode node, Field field)
    {
        return node.name.equals(field.getName())
                && Type.getType(field.getType()).getDescriptor().equals(node.desc)
                && field.getDeclaringClass().getName().replace(".", "/").equals(node.owner);
    }

    public static void compareStrings(String s, String s2)
    {
        System.out.println((s + " = " + s2 + "  -  ") + s.equals(s2));
    }

    public static String[] getExceptions(Class<?>[] exceptions)
    {
        if(exceptions.length == 0) return null;

        String[] strings = new String[exceptions.length];

        for(int i = 0; i < exceptions.length ; i++)
        {
            strings[i] = exceptions[i].getName().replace(".", "/");
        }

        return strings;
    }

    public static void setFinal(Field field, Object instance, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(instance, newValue);
    }

    public static void replaceStrings(Object object, String from, String to, boolean finals)
    {
        for(Field field : object.getClass().getFields())
        {
            try
            {
                if(Object[].class.isAssignableFrom(field.getType()))
                {
                    for(Object ob : (Object[]) field.get(object))
                    {
                        replaceStrings(ob, from, to, finals);
                    }
                }

                if(!field.getType().equals(String.class)) continue;

                if(!field.isAccessible()) field.setAccessible(true);
                String val = ((String) field.get(object)).replace(from, to);

                if(Modifier.isFinal(field.getModifiers()))
                {
                    if(!finals) continue;
                    setFinal(field, object, val);
                }
                else
                {
                    field.set(object, val);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public static MethodInsnNode fromMethodNode(int opcode, String owner, MethodNode node)
    {
        return new MethodInsnNode(
                opcode,
                owner,
                node.name,
                node.desc
        );
    }

    public static FieldInsnNode fromFieldNode(int opcode, String owner, FieldNode node)
    {
        return new FieldInsnNode(
                opcode,
                owner,
                node.name,
                node.desc
        );
    }

}
