package com.joojn.mixins.element;

import com.joojn.mixins.annotation.Node;
import com.joojn.mixins.annotation.Shadow;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ShadowMethod extends ShadowElement {

    public ShadowMethod(Method method, ClassNode cn)
    {
        Node node = method.getAnnotation(Shadow.class).node();

        this.desc = node.desc().isEmpty() ? Type.getMethodDescriptor(method) : node.desc();
        this.originalName = method.getName();

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
    public String originalName() {
        return this.originalName;
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

}
