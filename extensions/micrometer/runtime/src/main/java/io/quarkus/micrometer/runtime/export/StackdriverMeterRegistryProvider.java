package io.quarkus.micrometer.runtime.export;

import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.config.validate.Validated;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import io.quarkus.arc.DefaultBean;
import io.quarkus.micrometer.runtime.MicrometerRecorder;

@Singleton
public class StackdriverMeterRegistryProvider {
    private static final Logger log = Logger.getLogger(StackdriverMeterRegistryProvider.class);

    static final String PREFIX = "quarkus.micrometer.export.stackdriver.";
    static final String PUBLISH = "stackdriver.publish";
    static final String ENABLED = "stackdriver.enabled";

    @Produces
    @Singleton
    @DefaultBean
    public StackdriverConfig configure(Config config) throws Throwable {
        final Map<String, String> properties = MicrometerRecorder.captureProperties(config, PREFIX);

        // Special check: if publish is set, override the value of enabled
        // Specifically, the stackdriver registry must be enabled for this
        // Provider to even be present. If this instance (at runtime) wants
        // to prevent metrics from being published, then it would set
        // quarkus.micrometer.export.stackdriver.publish=false
        if (properties.containsKey(PUBLISH)) {
            properties.put(ENABLED, properties.get(PUBLISH));
        }

        StackdriverConfig sdConfig = new StackdriverConfig() {
            @Override
            public String get(String key) {
                return properties.get(key);
            }
        };

        Validated validated = sdConfig.validate();
        List<Validated.Invalid<?>> errors = validated.failures();
        if (validated.isInvalid()) {
            errors.stream().forEach(x -> {
                log.error(x.getMessage(), x.getException());
            });
        }
        return sdConfig;
    }

    @Produces
    @Singleton
    public StackdriverMeterRegistry registry(StackdriverConfig config, Clock clock) {
        return new StackdriverMeterRegistry(config, clock);
    }
}
