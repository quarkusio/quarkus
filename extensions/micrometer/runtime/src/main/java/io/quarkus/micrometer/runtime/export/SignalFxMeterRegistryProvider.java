package io.quarkus.micrometer.runtime.export;

import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Clock;
import io.micrometer.signalfx.SignalFxConfig;
import io.micrometer.signalfx.SignalFxMeterRegistry;
import io.micrometer.signalfx.SignalFxNamingConvention;
import io.quarkus.arc.DefaultBean;

@Singleton
public class SignalFxMeterRegistryProvider {
    private static final Logger log = Logger.getLogger(SignalFxMeterRegistryProvider.class);

    static final String PREFIX = "quarkus.micrometer.export.signalfx.";
    static final String PUBLISH = "signalfx.publish";
    static final String ENABLED = "signalfx.enabled";

    @Produces
    @Singleton
    @DefaultBean
    public SignalFxConfig configure(Config config) {
        final Map<String, String> properties = ConfigAdapter.captureProperties(config, PREFIX);

        return ConfigAdapter.validate(new SignalFxConfig() {
            @Override
            public String get(String key) {
                return properties.get(key);
            }
        });
    }

    @Produces
    @DefaultBean
    public SignalFxNamingConvention namingConvention() {
        return new SignalFxNamingConvention();
    }

    @Produces
    @Singleton
    public SignalFxMeterRegistry registry(SignalFxConfig config, Clock clock) {
        return new SignalFxMeterRegistry(config, clock);
    }
}
