package io.quarkus.runtime;

import java.util.List;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ClassPathSystemPropertyRecorder {

    public void set(List<String> allJarPaths) {
        System.setProperty("java.class.path", String.join(":", allJarPaths));
    }
}
