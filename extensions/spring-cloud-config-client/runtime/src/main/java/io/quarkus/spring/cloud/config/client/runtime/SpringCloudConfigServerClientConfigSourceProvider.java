package io.quarkus.spring.cloud.config.client.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.TlsConfig;

public class SpringCloudConfigServerClientConfigSourceProvider implements ConfigSourceProvider {

    private static final Logger log = Logger.getLogger(SpringCloudConfigServerClientConfigSourceProvider.class);

    private final SpringCloudConfigClientConfig springCloudConfigClientConfig;
    private final String applicationName;
    private final String activeProfile;

    private final SpringCloudConfigClientGateway springCloudConfigClientGateway;

    public SpringCloudConfigServerClientConfigSourceProvider(SpringCloudConfigClientConfig springCloudConfigClientConfig,
            String applicationName,
            String activeProfile, TlsConfig tlsConfig) {
        this.springCloudConfigClientConfig = springCloudConfigClientConfig;
        this.applicationName = applicationName;
        this.activeProfile = activeProfile;

        springCloudConfigClientGateway = new VertxSpringCloudConfigGateway(springCloudConfigClientConfig, tlsConfig);
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        List<ConfigSource> result = new ArrayList<>();
        try {
            boolean connectionTimeoutIsGreaterThanZero = !springCloudConfigClientConfig.connectionTimeout.isNegative()
                    && !springCloudConfigClientConfig.connectionTimeout.isZero();
            boolean readTimeoutIsGreaterThanZero = !springCloudConfigClientConfig.readTimeout.isNegative()
                    && !springCloudConfigClientConfig.readTimeout.isZero();
            Response response;
            // Check if configured timeouts are greater than zero in order to avoid an exception on atMost method
            if (connectionTimeoutIsGreaterThanZero || readTimeoutIsGreaterThanZero)
                response = springCloudConfigClientGateway.exchange(applicationName, activeProfile).await()
                        .atMost(springCloudConfigClientConfig.connectionTimeout
                                .plus(springCloudConfigClientConfig.readTimeout.multipliedBy(2)));
            else {
                response = springCloudConfigClientGateway.exchange(applicationName, activeProfile).await().indefinitely();
            }
            if (response != null) {
                final List<Response.PropertySource> propertySources = response.getPropertySources();
                Collections.reverse(propertySources); // reverse the property sources so we can increment the ordinal from lower priority to higher
                for (int i = 0; i < propertySources.size(); i++) {
                    final Response.PropertySource propertySource = propertySources.get(i);
                    // Property sources obtained from Spring Cloud Config are expected to have a higher priority than even system properties
                    // 400 is the ordinal of SysPropConfigSource, so we use 450 here
                    result.add(new InMemoryConfigSource(450 + i, propertySource.getName(),
                            propertySource.getSource()));
                }
            }
        } catch (Exception e) {
            final String errorMessage = "Unable to obtain configuration from Spring Cloud Config Server at "
                    + springCloudConfigClientConfig.url;
            if (springCloudConfigClientConfig.failFast) {
                throw new RuntimeException(errorMessage, e);
            } else {
                log.error(errorMessage, e);
                return Collections.emptyList();
            }
        } finally {
            springCloudConfigClientGateway.close();
        }
        return result;

    }

    private static final class InMemoryConfigSource implements ConfigSource {

        private final Map<String, String> values = new HashMap<>();
        private final int ordinal;
        private final String name;

        private InMemoryConfigSource(int ordinal, String name, Map<String, String> source) {
            this.ordinal = ordinal;
            this.name = name;
            this.values.putAll(source);
        }

        @Override
        public Map<String, String> getProperties() {
            return values;
        }

        @Override
        public Set<String> getPropertyNames() {
            return values.keySet();
        }

        @Override
        public int getOrdinal() {
            return ordinal;
        }

        @Override
        public String getValue(String propertyName) {
            return values.get(propertyName);
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
