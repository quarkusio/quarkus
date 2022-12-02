package io.quarkus.test.common;

/**
 * A wrapper around ClassLoader whose only purpose is to expose defineClass
 * This is needed in order to easily inject classes into the classloader
 * without having to resort to tricks (that don't work that well on new JDKs)
 */
public class DefineClassVisibleClassLoader extends ClassLoader {

    public DefineClassVisibleClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> visibleDefineClass(String name, byte[] b, int off, int len) throws ClassFormatError {
        return super.defineClass(name, b, off, len);
    }
}
