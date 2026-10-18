package io.quarkus.test.junit.classloading;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * If we set a parent, vertx http deployment QuarkusTestTypeTestCase and various others break.
 * If we don't set a parent and keep the parent as null, dev mode tests break such as test-test-profile break.
 * The solution is to have a parent, but do parent-last classloading.
 */
public class ParentLastURLClassLoader extends URLClassLoader {

    private final ClassLoader fallbackParent;

    public ParentLastURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, null);
        this.fallbackParent = parent;
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        // Do parent last
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            if (fallbackParent != null) {
                return fallbackParent.loadClass(name);
            } else {
                throw e;
            }
        }
    }
}
