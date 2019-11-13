package io.quarkus.quartz.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class QuartzRecorder {

    public void initialize(QuartzRuntimeConfig runtimeConfig, BeanContainer container) {
        QuartzSupport support = container.instance(QuartzSupport.class);
        support.initialize(runtimeConfig);
    }

}
