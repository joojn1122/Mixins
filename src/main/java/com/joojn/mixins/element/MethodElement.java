package com.joojn.mixins.element;

import com.joojn.mixins.annotation.MixinMethod;
import com.joojn.mixins.exception.MixinException;
import com.joojn.mixins.util.MixinHelper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Method;

public class MethodElement {

    public static MethodNode findMethod(ClassNode cn, Method method, MixinMethod annotation) throws MixinException
    {
        String name = annotation.desc().name().isEmpty() ? method.getName() : annotation.desc().name();
        String desc = annotation.desc().desc().isEmpty() ? Type.getMethodDescriptor(method) : annotation.desc().desc();

        for(MethodNode node : cn.methods)
        {
            if(
                    MixinHelper.argsEquals(
                        Type.getArgumentTypes(method),
                        node.desc
                    ) && name.equals(node.name)
            ) return node;
        }

        throw new MixinException("Could not find method with name '%s' and desc '%s' in class %s", name, desc, cn.name);
    }
}
