package io.quarkus.bootstrap;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * A wrapper around URLClassLoader whose only purpose is to expose defineClass
 * This is needed in order to easily inject classes into the classloader
 * without having to resort to tricks (that don't work that well on new JDKs)
 */
public class DefineClassVisibleURLClassLoader extends URLClassLoader {

    public DefineClassVisibleURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public Class<?> visibleDefineClass(String name, byte[] b, int off, int len) throws ClassFormatError {
        return super.defineClass(name, b, off, len);
    }
}
