package com.joojn.mixins.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

public class PrintUtil {

    public static void printInstructions(InsnList list)
    {
        for(AbstractInsnNode node : list)
        {
            if(node instanceof MethodInsnNode)
            {
                MethodInsnNode method = (MethodInsnNode) node;
//                System.out.println(node.getClass().getSimpleName());

                System.out.println("Method: " + method.name);

                System.out.println(method.desc);
                System.out.println(method.owner);
                System.out.println(method.getOpcode());
                System.out.println("-----");
            }

        }
    }

}
