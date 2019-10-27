package io.quarkus.bootstrap;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

/**
 * A wrapper around URLClassLoader whose only purpose is to expose defineClass
 * This is needed in order to easily inject classes into the classloader
 * without having to resort to tricks (that don't work that well on new JDKs)
 */
public class DefineClassVisibleURLClassLoader extends URLClassLoader {

    private static final List<String> KNOWN_LEAKING_PACKAGES_FROM_PARENT_CL = Arrays.asList(
            "org.apache.commons.lang3", // commons-lang3 used internally by gradle
            "com.google.common" // guava added by maven internals
    );

    public DefineClassVisibleURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public Class<?> visibleDefineClass(String name, byte[] b, int off, int len) throws ClassFormatError {
        return super.defineClass(name, b, off, len);
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        boolean lookup = false;
        for (String prefix : KNOWN_LEAKING_PACKAGES_FROM_PARENT_CL) {
            if (name.startsWith(prefix)) {
                lookup = true;
                break;
            }
        }

        // The parent classloader can introduce plenty of build system dependencies (like commons-lang3 or guava)
        // Which could be incompatible with what the user dependencies specify
        // The temporary solution is to not delegate to parent class loader for dependencies that are known
        // to leak in from the build system.
        // The solution is pretty bad and should be views as temporary until a proper solution
        // (like the one proposed here: https://github.com/quarkusio/quarkus/issues/770)
        if (lookup) {
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass == null) {
                try {
                    loadedClass = findClass(name);
                } catch (ClassNotFoundException ignored) {
                }

                if (loadedClass == null) {
                    loadedClass = super.loadClass(name);
                }
            }
            return loadedClass;
        }
        return super.loadClass(name);
    }
}
