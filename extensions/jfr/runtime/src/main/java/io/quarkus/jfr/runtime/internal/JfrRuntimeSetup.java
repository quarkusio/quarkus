package io.quarkus.jfr.runtime.internal;

import org.jboss.logging.Logger;

import io.quarkus.jfr.runtime.internal.config.JfrRuntimeConfig;
import io.quarkus.jfr.runtime.internal.http.rest.RestEndEvent;
import io.quarkus.jfr.runtime.internal.http.rest.RestPeriodEvent;
import io.quarkus.jfr.runtime.internal.http.rest.RestStartEvent;
import io.quarkus.jfr.runtime.internal.http.rest.client.RestClientPeriodEvent;
import jdk.jfr.FlightRecorder;

public final class JfrRuntimeSetup {

    private static final Logger LOG = Logger.getLogger(JfrRuntimeSetup.class);

    private JfrRuntimeSetup() {
    }

    public static void disableEvents(JfrRuntimeConfig runtimeConfig) {
        if (!runtimeConfig.enabled()) {
            LOG.info("quarkus-jfr is disabled at runtime");
            disableQuarkusJfr();
        } else {
            if (!runtimeConfig.restEnabled()) {
                LOG.info("quarkus-jfr for REST server is disabled at runtime");
                disableRestJfr();
            }
            if (!runtimeConfig.restClientEnabled()) {
                LOG.info("quarkus-jfr for REST Client is disabled at runtime");
                disableRestClientJfr();
            }
        }
    }

    private static void disableRestJfr() {
        FlightRecorder.unregister(RestStartEvent.class);
        FlightRecorder.unregister(RestEndEvent.class);
        FlightRecorder.unregister(RestPeriodEvent.class);
    }

    private static void disableRestClientJfr() {
        FlightRecorder.unregister(RestClientPeriodEvent.class);
    }

    private static void disableQuarkusJfr() {
        disableRestJfr();
        disableRestClientJfr();
    }
}
