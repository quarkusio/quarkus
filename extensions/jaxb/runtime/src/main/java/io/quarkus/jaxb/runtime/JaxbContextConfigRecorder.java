package io.quarkus.jaxb.runtime;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JaxbContextConfigRecorder {
    private volatile static Set<String> classesToBeBound = new HashSet<>();

    public void addClassesToBeBound(Collection<String> additionalClassesToBeBound) {
        this.classesToBeBound.addAll(additionalClassesToBeBound);
    }

    public static String[] getClassesToBeBound() {
        return classesToBeBound.toArray(new String[0]);
    }
}
