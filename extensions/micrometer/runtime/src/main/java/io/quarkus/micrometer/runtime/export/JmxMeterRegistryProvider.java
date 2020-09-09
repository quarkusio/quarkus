package io.quarkus.micrometer.runtime.export;

import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;
import io.micrometer.jmx.JmxConfig;
import io.micrometer.jmx.JmxMeterRegistry;
import io.quarkus.arc.DefaultBean;
import io.quarkus.micrometer.runtime.MicrometerRecorder;

@Singleton
public class JmxMeterRegistryProvider {
    static final String PREFIX = "quarkus.micrometer.export.jmx.";

    @Produces
    @Singleton
    @DefaultBean
    public HierarchicalNameMapper config() {
        return HierarchicalNameMapper.DEFAULT;
    }

    @Produces
    @Singleton
    @DefaultBean
    public JmxConfig configure(Config config) {
        final Map<String, String> properties = MicrometerRecorder.captureProperties(config, PREFIX);

        return new JmxConfig() {
            @Override
            public String get(String key) {
                return properties.get(key);
            }
        };
    }

    @Produces
    @Singleton
    public JmxMeterRegistry registry(JmxConfig config, Clock clock, HierarchicalNameMapper nameMapper) {
        return new JmxMeterRegistry(config, clock, nameMapper);
    }
}
