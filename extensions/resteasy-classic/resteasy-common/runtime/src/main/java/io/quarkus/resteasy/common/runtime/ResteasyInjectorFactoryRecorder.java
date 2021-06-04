package io.quarkus.resteasy.common.runtime;

import org.jboss.resteasy.spi.InjectorFactory;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ResteasyInjectorFactoryRecorder {

    public RuntimeValue<InjectorFactory> setup() {
        return new RuntimeValue<>(new QuarkusInjectorFactory());
    }
}
