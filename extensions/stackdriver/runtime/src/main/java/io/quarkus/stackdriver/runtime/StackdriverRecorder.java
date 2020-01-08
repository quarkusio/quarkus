package io.quarkus.stackdriver.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.stackdriver.runtime.configuration.StackdriverConfiguration;

@Recorder
public class StackdriverRecorder {

    public void runtimeConfiguration(StackdriverConfiguration config) {
        ArcContainer container = Arc.container();
        container.instance(StackdriverConfigurer.class).get().setStackdriverConfiguration(config);
    }
}
