package io.quarkus.extest.runtime.def;

import jakarta.inject.Singleton;

public class ControllerConfigurationProducer {

    @Singleton
    public ControllerConfiguration produce() {
        return ControllerConfigurationRecorder.controllerConfiguration;
    }
}
