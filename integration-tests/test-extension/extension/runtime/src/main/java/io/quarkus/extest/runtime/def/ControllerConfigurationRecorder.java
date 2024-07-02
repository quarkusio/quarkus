package io.quarkus.extest.runtime.def;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ControllerConfigurationRecorder {

    public static volatile ControllerConfiguration controllerConfiguration;

    public void setControllerConfiguration(QuarkusControllerConfiguration controllerConfiguration) {
        ControllerConfigurationRecorder.controllerConfiguration = controllerConfiguration;
    }
}
