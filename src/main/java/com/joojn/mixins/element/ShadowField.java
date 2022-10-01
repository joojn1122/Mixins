package com.joojn.mixins.element;

import com.joojn.mixins.annotation.Node;
import com.joojn.mixins.annotation.Shadow;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Field;

public class ShadowField extends ShadowElement {

    public ShadowField(Field field, ClassNode cn)
    {
        Node node = field.getAnnotation(Shadow.class).node();

        desc = Type.getDescriptor(field.getType());

        this.originalName = field.getName();

        if(node.name().isEmpty())
        {
            this.name = this.originalName;
        }
        else name = node.name();

        if(node.owner().isEmpty())
        {
            this.owner = cn.name;
        }
        else this.owner = node.owner().replace(".", "/");

        this.opcode = node.opcode();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDesc() {
        return this.desc;
    }

    @Override
    public String getOwner() {
        return this.owner;
    }

    @Override
    public int getOpcode() {
        return this.opcode;
    }

    @Override
    public String originalName() {
        return this.originalName;
    }
}
