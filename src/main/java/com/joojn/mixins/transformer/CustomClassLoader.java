package com.joojn.mixins.transformer;
import javafx.util.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CustomClassLoader extends ClassLoader {

    public static final CustomClassLoader INSTANCE = new CustomClassLoader();

    public Pair<Class<?>, byte[]> loadClassAndData(String name) throws ClassNotFoundException
    {
        byte[] bt = loadClassData(name);
        return new Pair<>(defineClass(name, bt, 0, bt.length), bt);
    }

    public Class<?> findClass(String name) throws ClassNotFoundException
    {
        byte[] bt = loadClassData(name);
        return defineClass(name, bt, 0, bt.length);
    }

    public byte[] loadClassData(String className) throws ClassNotFoundException{
        //read class
        InputStream is = getClass().getClassLoader().getResourceAsStream(className.replace(".", "/")+".class");

        if(is == null) throw new ClassNotFoundException();

        ByteArrayOutputStream byteSt = new ByteArrayOutputStream();
        //write into byte
        int len =0;
        try {
            while((len=is.read())!=-1){
                byteSt.write(len);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        //convert into byte array
        return byteSt.toByteArray();
    }

}
