package com.joojn.mixins.element;

public abstract class ShadowElement {

    protected String name;
    protected String originalName;
    protected String owner;
    protected String desc;
    protected int opcode;

    public abstract String getName();
    public abstract String originalName();
    public abstract String getDesc();
    public abstract String getOwner();
    public abstract int getOpcode();

    public String toString()
    {
        return String.format("ShadowElement{name=%s, desc=%s, owner=%s, opcode=%s}", getName(), getDesc(), getOwner(), getOpcode());
    }
}
