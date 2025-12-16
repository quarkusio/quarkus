package io.quarkus.jfr.runtime.internal;

import java.util.List;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.jfr.runtime.internal.config.JfrRuntimeConfig;
import io.quarkus.jfr.runtime.internal.http.rest.RestEndEvent;
import io.quarkus.jfr.runtime.internal.http.rest.RestPeriodEvent;
import io.quarkus.jfr.runtime.internal.http.rest.RestStartEvent;
import io.quarkus.jfr.runtime.internal.runtime.QuarkusRuntimeInfo;
import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigUtils;
import jdk.jfr.FlightRecorder;

@Recorder
public class JfrRecorder {
    private final RuntimeValue<JfrRuntimeConfig> runtimeConfig;

    public JfrRecorder(final RuntimeValue<JfrRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public void runtimeInit() {

        Logger logger = Logger.getLogger(JfrRecorder.class);

        if (!runtimeConfig.getValue().enabled()) {
            logger.info("quarkus-jfr is disabled at runtime");
            this.disabledQuarkusJfr();
        } else {
            if (!runtimeConfig.getValue().restEnabled()) {
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

    public Supplier<QuarkusRuntimeInfo> quarkusInfoSupplier(String version, List<String> features) {
        return new Supplier<>() {
            @Override
            public QuarkusRuntimeInfo get() {
                return new QuarkusRuntimeInfo() {
                    @Override
                    public String imageMode() {
                        return ImageMode.current().name();
                    }

                    @Override
                    public String profiles() {
                        return String.join(",", ConfigUtils.getProfiles());
                    }

                    @Override
                    public String version() {
                        return version;
                    }

                    @Override
                    public List<String> features() {
                        return features;
                    }
                };
            }
        };
    }
}
