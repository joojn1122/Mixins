package com.joojn.mixins.element;

import com.joojn.mixins.annotation.MixinMethod;
import com.joojn.mixins.util.MixinHelper;
import com.joojn.mixins.exception.MixinException;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Method;

public class NewMethod {

    public static MethodNode findMethod(
            ClassNode currentNode,
            Method method,
            String err
    ) throws MixinException
    {
        for(MethodNode node : currentNode.methods)
        {
            if(MixinHelper.methodEquals(node, method))
            {
                return node;
            }
        }

        throw new MixinException(err, method.getName());
    }
}
