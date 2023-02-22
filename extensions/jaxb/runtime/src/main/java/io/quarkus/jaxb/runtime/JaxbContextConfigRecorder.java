package io.quarkus.jaxb.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JaxbContextConfigRecorder {
    private volatile static Set<Class<?>> classesToBeBound = new HashSet<>();

    public void addClassesToBeBound(Collection<Class<?>> classes) {
        this.classesToBeBound.addAll(classes);
    }

    public static Set<Class<?>> getClassesToBeBound() {
        return Collections.unmodifiableSet(classesToBeBound);
    }

    public static boolean isClassBound(Class<?> clazz) {
        return classesToBeBound.contains(clazz.getName());
    }
}
