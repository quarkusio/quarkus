package io.quarkus.jfr.runtime;

import org.jboss.logging.Logger;

import io.quarkus.jfr.runtime.config.JfrRuntimeConfig;
import io.quarkus.jfr.runtime.http.rest.RestEndEvent;
import io.quarkus.jfr.runtime.http.rest.RestPeriodEvent;
import io.quarkus.jfr.runtime.http.rest.RestStartEvent;
import io.quarkus.runtime.annotations.Recorder;
import jdk.jfr.FlightRecorder;

@Recorder
public class JfrRecorder {

    public void runtimeInit(JfrRuntimeConfig runtimeConfig) {

        Logger logger = Logger.getLogger(JfrRecorder.class);

        if (!runtimeConfig.enabled()) {
            logger.info("quarkus-jfr is disabled at runtime");
            this.disabledQuarkusJfr();
        } else {
            if (!runtimeConfig.restEnabled()) {
                logger.info("quarkus-jfr for REST server is disabled at runtime");
                this.disabledRestJfr();
            }
        }
    }

    public void disabledRestJfr() {
        FlightRecorder.unregister(RestStartEvent.class);
        FlightRecorder.unregister(RestEndEvent.class);
        FlightRecorder.unregister(RestPeriodEvent.class);
    }

    public void disabledQuarkusJfr() {
        this.disabledRestJfr();
    }
}
