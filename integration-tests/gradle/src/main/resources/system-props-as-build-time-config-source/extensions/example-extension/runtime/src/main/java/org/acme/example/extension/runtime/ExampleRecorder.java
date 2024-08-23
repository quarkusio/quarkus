package org.acme.example.extension.runtime;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ExampleRecorder {

    public RuntimeValue<ExampleBuildOptions> buildOptions(String name) {
        System.out.println("ExampleRecorder.buildOptions " + name);
        return new RuntimeValue<>(new ExampleBuildOptions(name));
    }
}