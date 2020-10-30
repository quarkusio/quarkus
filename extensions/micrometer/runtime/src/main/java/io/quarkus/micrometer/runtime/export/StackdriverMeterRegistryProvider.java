package io.quarkus.micrometer.runtime.export;

import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Clock;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import io.micrometer.stackdriver.StackdriverNamingConvention;
import io.quarkus.arc.DefaultBean;

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
        final Map<String, String> properties = ConfigAdapter.captureProperties(config, PREFIX);

        // Special check: if publish is set, override the value of enabled
        // Specifically, the StackDriver registry must be enabled for this
        // Provider to even be present. If this instance (at runtime) wants
        // to prevent metrics from being published, then it would set
        // quarkus.micrometer.export.stackdriver.publish=false
        if (properties.containsKey(PUBLISH)) {
            properties.put(ENABLED, properties.get(PUBLISH));
        }

        return ConfigAdapter.validate(new StackdriverConfig() {
            @Override
            public String get(String key) {
                return properties.get(key);
            }
        });
    }

    @Produces
    @DefaultBean
    public StackdriverNamingConvention namingConvention() {
        return new StackdriverNamingConvention();
    }

    @Produces
    @Singleton
    public StackdriverMeterRegistry registry(StackdriverConfig config, Clock clock) {
        return StackdriverMeterRegistry.builder(config).clock(clock).build();
    }
}
