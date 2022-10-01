package com.joojn.mixins.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Desc {

    String name() default "";
    String desc() default "";

}
