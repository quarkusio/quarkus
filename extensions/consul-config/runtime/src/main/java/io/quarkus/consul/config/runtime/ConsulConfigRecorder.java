package io.quarkus.consul.config.runtime;

import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ConsulConfigRecorder {

    private static final Logger log = Logger.getLogger(ConsulConfigRecorder.class);

    final ConsulConfig consulConfig;

    public ConsulConfigRecorder(ConsulConfig consulConfig) {
        this.consulConfig = consulConfig;
    }

    public RuntimeValue<ConfigSourceProvider> configSources() {
        if (!consulConfig.enabled) {
            log.debug(
                    "No attempt will be made to obtain configuration from Consul because the functionality has been disabled via configuration");
            return emptyRuntimeValue();
        }

        return new RuntimeValue<>(
                new ConsulConfigSourceProvider(consulConfig));
    }

    private RuntimeValue<ConfigSourceProvider> emptyRuntimeValue() {
        return new RuntimeValue<>(new EmptyConfigSourceProvider());
    }

    private static class EmptyConfigSourceProvider implements ConfigSourceProvider {

        @Override
        public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
            return Collections.emptyList();
        }
    }
}
