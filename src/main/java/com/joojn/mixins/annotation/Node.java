package com.joojn.mixins.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Node {

    String owner() default "";
    Desc desc() default @Desc();
    int opcode() default -1;

}
