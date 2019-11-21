package io.quarkus.quartz.runtime;

import java.util.Optional;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuartzRecorder {

    public void initialize(QuartzRuntimeConfig runTimeConfig, QuartzBuildTimeConfig buildTimeConfig, BeanContainer container,
            Optional<String> driverDialect) {
        QuartzSupport support = container.instance(QuartzSupport.class);
        support.initialize(runTimeConfig, buildTimeConfig, driverDialect);
    }

}
