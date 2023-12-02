package io.quarkus.runtime;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ClassPathSystemPropertyRecorder {

    public void set(String value) {
        System.setProperty("java.class.path", value);
    }
}
