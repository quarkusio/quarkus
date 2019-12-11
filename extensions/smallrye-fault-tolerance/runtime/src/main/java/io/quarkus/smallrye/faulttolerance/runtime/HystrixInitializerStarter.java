package io.quarkus.smallrye.faulttolerance.runtime;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.faulttolerance.HystrixInitializer;

@Dependent
public class HystrixInitializerStarter {

    /**
     * This is a replacement for <code>io.smallrye.faulttolerance.HystrixInitializer.init(Object)</code> observer method which
     * is vetoed because we don't want initialize Hystrix during static init.
     * 
     * @param event
     * @param initializer
     */
    void startup(@Observes StartupEvent event, HystrixInitializer initializer) {
        // HystrixInitializer is a normal scoped bean so we have to invoke a method upon the injected proxy to force the instantiation
        initializer.toString();
    }

}
