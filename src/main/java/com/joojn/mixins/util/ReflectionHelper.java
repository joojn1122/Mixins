package com.joojn.mixins.util;

import javafx.util.Pair;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ReflectionHelper {

    /**
     * @param clazz Class in which you are calling super
     * @param name Method name
     * @param returnType Return type of method
     * @param args Method args
     * @return MethodHandle or null if not found
     */
    public static MethodHandle getSuperMethod(
            Class<?> clazz,
            String name,
            Class<?> returnType,
            Class<?>... args
    )
    {
        try
        {
            Field IMPL_LOOKUP = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            IMPL_LOOKUP.setAccessible(true);

            MethodHandles.Lookup lkp = (MethodHandles.Lookup) IMPL_LOOKUP.get(null);

            return lkp.findSpecial(clazz.getSuperclass(), name, MethodType.methodType(returnType, args), clazz);
        }
        catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static Class<?> getPrimitiveClass(Class<?> clazz)
    {
        HashMap<Class<?>, Class<?>> hash = new HashMap<>();

        hash.put(Integer.class, int.class);
        hash.put(Double.class, double.class);
        hash.put(Float.class, float.class);
        hash.put(Character.class, char.class);
        hash.put(Long.class, long.class);
        hash.put(Short.class, short.class);

        return hash.getOrDefault(clazz, clazz);
    }


    public static Object invoke(Method method, Object instance, Object... args)
    {
        try
        {
            if(!method.isAccessible()) method.setAccessible(true);

            return method.invoke(instance, args);
        }
        catch (InvocationTargetException | IllegalAccessException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static Object invokeSimple(Method method, Object instance)
    {
        return invoke(method, instance, (Object[]) null);
    }

    public static Object invokeStatic(Method method, Object... args)
    {
        return invoke(method, null, args);
    }

    public static Object invokeStatic(Method method)
    {
        return invokeSimple(method, null);
    }

    public static <T> T getField(Object instance, String name)
    {
        try
        {
            Field field = instance.getClass().getField(name);

            return getField(instance, field);
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static <T> T getField(Object instance, Field field)
    {
        try
        {
            if(!field.isAccessible()) field.setAccessible(true);

            return (T) field.get(instance);
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static String fieldsToString(Object instance) {

        StringBuilder stringBuilder = new StringBuilder().append("[");

        for(Field field : instance.getClass().getFields())
        {
            if(Modifier.isStatic(field.getModifiers())) continue;

            if(!field.isAccessible()) field.setAccessible(true);

            try
            {
                Object value = field.get(instance);
                stringBuilder.append(field.getName()).append(" = ").append(value).append(",");
            }
            catch (IllegalAccessException ignored) {} // can't happen


        }

        return stringBuilder.append("]").toString();
    }

    public static String getSignature(Method m)
    {
        String sig;
        try
        {
            Field gSig = Method.class.getDeclaredField("signature");
            gSig.setAccessible(true);
            sig = (String) gSig.get(m);

            if(sig != null) return sig;
        }
        catch (IllegalAccessException | NoSuchFieldException e)
        {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder("(");
        for(Class<?> c : m.getParameterTypes())
            sb.append((sig= Array.newInstance(c, 0).toString()), 1, sig.indexOf('@'));

        return sb.append(')')
                .append(
                        m.getReturnType() == void.class ? "V":
                        (sig=Array.newInstance(m.getReturnType(), 0).toString()).substring(1, sig.indexOf('@'))
                )
                .toString()
                .replace(".", "/");
    }

    public static void setField(Field field, Object instance, Object value)
    {
        try
        {
            if (!field.isAccessible()) field.setAccessible(true);

            if (Modifier.isFinal(field.getModifiers()))
            {
                Field modifierField = Field.class.getDeclaredField("modifiers");
                modifierField.setAccessible(true);

                modifierField.set(field, field.getModifiers() & ~Modifier.FINAL);
            }

            field.set(instance, value);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    public static void setField(Object instance, String name, Object value)
    {
        try
        {
            Field field = instance.getClass().getField(name);
            setField(field, instance, value);
        }
        catch (NoSuchFieldException e)
        {
            e.printStackTrace();
        }
    }

    public static void setAnyField(Object instance, String name, Object value)
    {
        Field field = findAnyField(instance.getClass(), name);
        setField(field, instance, value);
    }

    /**
     *
     * @param instance Instance
     * @param name Name of method
     * @param params Classes and params = (int.class, 1, float.class, 2f) etc.
     * @return T
     * @param <T> T
     */
    public static <T> T invokeAnyMethod(Object instance, String name, Object... params)
    {
        Set<Pair<Class<?>, Object>> args = getArgs(params);
        if(args == null) throw new NullPointerException("Invalid params passed");

        return (T) ReflectionHelper.invoke(
                ReflectionHelper.findAnyMethod(
                        instance.getClass(),
                        name,
                        args
                        .stream()
                        .map(Pair::getKey)
                        .toArray(Class<?>[]::new)
                ),
                instance,
                args
                .stream()
                .map(Pair::getValue)
                .toArray(Object[]::new)
        );
    }

    public static Set<Pair<Class<?>, Object>> getArgs(Object... params)
    {
        if(params.length % 2 != 0) return null;

        Set<Pair<Class<?>, Object>> set = new HashSet<>();
        Class<?> current = null;

        for(int i = 0; i < params.length ; i++)
        {
            if(i % 2 == 0)
            {
                current = (Class<?>) params[i];
            }
            else
            {
                set.add(new Pair<>(current, params[i]));
            }
        }

        return set;
    }

    public static Field findAnyField(Class<?> clazz, String name)
    {
        final Field[] field = {null};

        walkClass(clazz,
                (clazz_) -> {

                    try
                    {
                        field[0] = clazz_.getDeclaredField(name);
                        return true;
                    }
                    catch (NoSuchFieldException ignored)
                    {
                        return false;
                    }
                }
           );

        return field[0];
    }

    public static Method findAnyMethod(Class<?> clazz, String name, Class<?>... params)
    {
        final Method[] method = {null};

        walkClass(clazz,
                (clazz_) -> {
                    try
                    {
                        method[0] = clazz_.getDeclaredMethod(name, params);
                        return true;
                    }
                    catch (NoSuchMethodException ignored)
                    {
                        return false;
                    }
                }
        );

        return method[0];
    }

    public interface classWalker{
        boolean walk(Class<?> clazz);
    }

    /**
     * Let you walk through all super classes of class until it reaches Object class
     * @param clazz Class to walk through
     * @param walker Class walker (Something like foreach), but if returned true the walker will stop walking
     */
    public static void walkClass(Class<?> clazz, classWalker walker)
    {
        Class<?> current = clazz;

        do
        {
            if(walker.walk(current)) return;
        }
        while((current = current.getSuperclass()) != Object.class);
    }

}
