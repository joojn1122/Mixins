package com.joojn.mixins.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Reflector {

    public interface MethodSelector {
        boolean select(Method method);
    }

    public interface FieldSelector {
        boolean select(Field field);
    }


    /**
        Invokes method from selected class

        @param clazz Target class
        @param methodSelector Selector for method
        @param indexOfMethod Which method should be selected,
        example: MethodSelector -> return method.getName().equals("cc"), index = 2 == select 3rd method with name cc
        @param instance Instance of class
        @param args Method args

        @return Object or null if not found / error
     */
    public static <T> T invokeMethod(Class<?> clazz,
                              MethodSelector methodSelector,
                              int indexOfMethod,
                              Object instance,
                              Object... args)
    {
        int currentIndex = 0;

        for(Method method : clazz.getMethods())
        {
            if(methodSelector.select(method))
            {
                if(currentIndex == indexOfMethod) return (T) ReflectionHelper.invoke(method, instance, args);

                currentIndex++;
            }
        }

        return null;
    }

    public static <T> T invokeMethod(Class<?> clazz,
                                     MethodSelector methodSelector,
                                     Object instance,
                                     Object... args)
    {
        return invokeMethod(clazz, methodSelector, 0, instance, args);
    }

    public static <T> T invokeStaticMethod(Class<?> clazz,
                                    MethodSelector methodSelector,
                                    int indexOfMethod,
                                    Object... args
                                    )
    {
        return invokeMethod(clazz, methodSelector, indexOfMethod, null, args);
    }

    public static <T> T invokeStaticMethod(Class<?> clazz,
                                           MethodSelector methodSelector,
                                           Object... args
    )
    {
        return invokeMethod(clazz, methodSelector, null, args);
    }

    /**
     Gets field from selected class

     @return Object or null if not found / error
     */
    public static <T> T getField(Class<?> clazz,
                                 FieldSelector selector,
                                 int indexOfField,
                                 Object instance)
    {
        int currentIndex = 0;

        for(Field field : clazz.getFields())
        {
            if(selector.select(field))
            {
                if(indexOfField == currentIndex)
                {
                    try
                    {
                        if(!field.isAccessible())
                            field.setAccessible(true);

                        return (T) field.get(instance);
                    }
                    catch (IllegalAccessException e)
                    {
                        e.printStackTrace();
                        return null;
                    }
                }

                currentIndex++;
            }
        }

        return null;
    }

    public static <T> T getField(Class<?> clazz,
                                 FieldSelector selector,
                                 Object instance)
    {
        return getField(clazz, selector, 0, instance);
    }

    public static <T> T getStaticField(Class<?> clazz,
                                       FieldSelector selector,
                                       int indexOfField)
    {
        return getField(clazz, selector, indexOfField, null);
    }

    public static <T> T getStaticField(Class<?> clazz,
                                       FieldSelector selector)
    {
        return getField(clazz, selector, null);
    }
}
