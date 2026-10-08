package io.quarkus.jaxb.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JaxbContextConfigRecorder {
    private volatile static Set<Class<?>> classesToBeBound = new HashSet<>();

    public static void addClassesToBeBound(Collection<Class<?>> classes) {
        classesToBeBound.addAll(classes);
    }

    public static void reset() {
        classesToBeBound.clear();
    }

    public static void bindClasses(List<String> classNames) {
        reset();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String className : classNames) {
            try {
                classesToBeBound.add(Class.forName(className, false, classLoader));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to load class " + className + " to be bound to the JAXB context", e);
            }
        }
    }

    public static Set<Class<?>> getClassesToBeBound() {
        return Collections.unmodifiableSet(classesToBeBound);
    }

    public static boolean isClassBound(Class<?> clazz) {
        return classesToBeBound.contains(clazz);
    }
}
