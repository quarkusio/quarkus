package io.quarkus.test.common;

import java.lang.reflect.Method;
import java.util.Collection;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * Groovy maintains a cache that causes memory leaks
 *
 * We need to manually clear it if present
 */
public class GroovyCacheCleaner {

    public static void clearGroovyCache() {
        try {
            Class<?> clazz = Class.forName("org.codehaus.groovy.reflection.ClassInfo", true,
                    GroovyCacheCleaner.class.getClassLoader());
            Method getTheClass = clazz.getDeclaredMethod("getTheClass");
            Method remove = clazz.getDeclaredMethod("remove", Class.class);
            Collection<?> info = (Collection<?>) clazz.getDeclaredMethod("getAllClassInfo").invoke(null);
            for (Object obj : info) {
                Class theClass = (Class) getTheClass.invoke(obj);
                ClassLoader classLoader = theClass.getClassLoader();
                if (classLoader instanceof QuarkusClassLoader && ((QuarkusClassLoader) classLoader).isClosed()) {
                    remove.invoke(null, theClass);
                }
            }
        } catch (Exception exception) {
            return;
        }
    }

}
