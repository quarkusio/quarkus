package io.quarkus.spring.cloud.config.client.runtime;

import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ProfileManager;

@Recorder
public class SpringCloudConfigClientRecorder {

    private static final Logger log = Logger.getLogger(SpringCloudConfigClientRecorder.class);

    public RuntimeValue<ConfigSourceProvider> create(SpringCloudConfigClientConfig springCloudConfigClientConfig,
            ApplicationConfig applicationConfig, TlsConfig tlsConfig) {
        if (!springCloudConfigClientConfig.enabled) {
            log.debug(
                    "No attempt will be made to obtain configuration from the Spring Cloud Config Server because the functionality has been disabled via configuration");
            return emptyRuntimeValue();
        }

        if (!applicationConfig.name.isPresent()) {
            log.warn(
                    "No attempt will be made to obtain configuration from the Spring Cloud Config Server because the application name has not been set. Consider setting it via 'quarkus.application.name'");
            return emptyRuntimeValue();
        }

        return new RuntimeValue<>(new SpringCloudConfigServerClientConfigSourceProvider(
                springCloudConfigClientConfig, applicationConfig.name.get(), ProfileManager.getActiveProfile(), tlsConfig));
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
