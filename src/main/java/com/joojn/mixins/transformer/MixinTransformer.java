package com.joojn.mixins.transformer;

import com.joojn.mixins.MixinMain;
import com.joojn.mixins.exception.MixinException;

import javafx.util.Pair;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashMap;

public class MixinTransformer implements ClassFileTransformer {

    public static final HashMap<String, byte[]> usedClasses = new HashMap<>();

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    )
    {
        // to prevent load one class multiple times
        if(usedClasses.containsKey(className)) {
            return classfileBuffer;
        }

        usedClasses.put(className, classfileBuffer);

        Pair<Class<?>, byte[]> mixin = MixinMain.getByName(className);

        if(mixin == null) return classfileBuffer;

        try
        {
            byte[] buffer = MixinMain.transformClass(mixin, classfileBuffer);
            usedClasses.put(className, buffer);

            return buffer;
        }
        catch (MixinException e)
        {
            MixinMain.err(e);
        }

        return classfileBuffer;
    }
}
