package io.quarkus.jaxb.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JaxbContextConfigRecorder {
    private volatile static Set<Class<?>> classesToBeBound = new HashSet<>();

    public static void addClassesToBeBound(Collection<Class<?>> classes) {
        classesToBeBound.addAll(classes);
    }

    public static void reset() {
        classesToBeBound.clear();
    }

    public static void bindClasses(Collection<Class<?>> classes) {
        reset();
        addClassesToBeBound(classes);
    }

    public static Set<Class<?>> getClassesToBeBound() {
        return Collections.unmodifiableSet(classesToBeBound);
    }

    public static boolean isClassBound(Class<?> clazz) {
        return classesToBeBound.contains(clazz);
    }
}
