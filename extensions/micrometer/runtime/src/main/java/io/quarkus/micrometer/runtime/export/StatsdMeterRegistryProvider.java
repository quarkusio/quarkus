package io.quarkus.micrometer.runtime.export;

import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Clock;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdMeterRegistry;
import io.quarkus.arc.DefaultBean;

@Singleton
public class StatsdMeterRegistryProvider {
    private static final Logger log = Logger.getLogger(StatsdMeterRegistryProvider.class);

    static final String PREFIX = "quarkus.micrometer.export.statsd.";
    static final String PUBLISH = "statsd.publish";
    static final String ENABLED = "statsd.enabled";

    @Produces
    @Singleton
    @DefaultBean
    public StatsdConfig configure(Config config) {
        final Map<String, String> properties = ConfigAdapter.captureProperties(config, PREFIX);

        // Special check: if publish is set, override the value of enabled
        // Specifically, the StatsD registry must be enabled for this
        // Provider to even be present. If this instance (at runtime) wants
        // to prevent metrics from being published, then it would set
        // quarkus.micrometer.export.statsd.publish=false
        if (properties.containsKey(PUBLISH)) {
            properties.put(ENABLED, properties.get(PUBLISH));
        }

        return ConfigAdapter.validate(new StatsdConfig() {
            @Override
            public String get(String key) {
                return properties.get(key);
            }
        });
    }

    @Produces
    @Singleton
    public StatsdMeterRegistry registry(StatsdConfig config, Clock clock) {
        return new StatsdMeterRegistry(config, clock);
    }
}
