package io.quarkus.observation;

import io.micrometer.observation.ObservationRegistry;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.observation.config.ObservationRuntimeConfig;
import io.quarkus.observation.handler.PrintOutHandler;

public class ObservationRecorder {

    public static void registerPrintOutHandler(BeanContainer beanContainer, ObservationRuntimeConfig runtimeConfig) {
        if (runtimeConfig.printOut()) {
            ObservationRegistry registry = beanContainer.beanInstance(ObservationRegistry.class);
            registry.observationConfig().observationHandler(new PrintOutHandler());
        }
    }

}
