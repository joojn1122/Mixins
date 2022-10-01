package net.mixins;

import com.joojn.mixins.MixinMain;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

public class Main {

    public static void premain(String arg, Instrumentation inst)
    {
        try
        {
            MixinMain.loadMixins(inst);
        }
        catch (ClassNotFoundException | IOException e)
        {
            e.printStackTrace();
        }
    }
}