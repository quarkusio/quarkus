package io.quarkus.spring.cloud.config.client.runtime;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.spring.cloud.config.client.runtime.Response.PropertySource;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory.ConfigurableConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.common.MapBackedConfigSource;

public class SpringCloudConfigClientConfigSourceFactory
        implements ConfigurableConfigSourceFactory<SpringCloudConfigClientConfig> {
    private static final Logger log = Logger.getLogger(SpringCloudConfigClientConfigSourceFactory.class);

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context,
            final SpringCloudConfigClientConfig config) {
        List<ConfigSource> sources = new ArrayList<>();

        if (!config.enabled()) {
            log.debug(
                    "No attempt will be made to obtain configuration from the Spring Cloud Config Server because the functionality has been disabled via configuration");
            return sources;
        }

        ConfigValue applicationName = context.getValue("quarkus.application.name");
        if (applicationName == null || applicationName.getValue() == null) {
            log.warn(
                    "No attempt will be made to obtain configuration from the Spring Cloud Config Server because the application name has not been set. Consider setting it via 'quarkus.application.name'");
            return sources;
        }

        boolean connectionTimeoutIsGreaterThanZero = !config.connectionTimeout().isNegative()
                && !config.connectionTimeout().isZero();
        boolean readTimeoutIsGreaterThanZero = !config.readTimeout().isNegative() && !config.readTimeout().isZero();

        VertxSpringCloudConfigGateway client = new VertxSpringCloudConfigGateway(config);
        try {
            List<Response> responses = new ArrayList<>();
            for (String profile : determineProfiles(context, config)) {
                Response response;
                if (connectionTimeoutIsGreaterThanZero || readTimeoutIsGreaterThanZero) {
                    response = client.exchange(applicationName.getValue(), profile).await()
                            .atMost(config.connectionTimeout().plus(config.readTimeout().multipliedBy(2)));
                } else {
                    response = client.exchange(applicationName.getValue(), profile).await().indefinitely();
                }

                if (response.getProfiles().contains(profile)) {
                    responses.add(response);
                }
            }

            int ordinal = 450;
            // Profiles are looked from the highest ordinal to lowest, so we reverse the collection to build the source list
            Collections.reverse(responses);
            for (Response response : responses) {
                List<PropertySource> propertySources = response.getPropertySources();
                // Same reverse rule here
                Collections.reverse(propertySources);

                for (PropertySource propertySource : propertySources) {
                    sources.add(SpringCloudPropertySource.from(propertySource, response.getProfiles(), ordinal++));
                }
            }

            return sources;

        } catch (Exception e) {
            final String errorMessage = "Unable to obtain configuration from Spring Cloud Config Server at " + config.url();
            if (config.failFast()) {
                throw new RuntimeException(errorMessage, e);
            } else {
                log.error(errorMessage, e);
                return emptyList();
            }
        } finally {
            client.close();
        }
    }

    private static List<String> determineProfiles(ConfigSourceContext context, SpringCloudConfigClientConfig config) {
        if (config.profiles().isPresent()) {
            return config.profiles().get();
        }
        return context.getProfiles();
    }

    private static class SpringCloudPropertySource extends MapBackedConfigSource {
        private SpringCloudPropertySource(final String name, final Map<String, String> propertyMap, final int defaultOrdinal) {
            super(name, propertyMap, defaultOrdinal);
        }

        static SpringCloudPropertySource from(PropertySource propertySource, List<String> profiles, int ordinal) {
            Map<String, String> values = new HashMap<>();
            Map<String, String> source = propertySource.getSource();
            for (String profile : profiles) {
                for (Map.Entry<String, String> entry : source.entrySet()) {
                    values.put("%" + profile + "." + entry.getKey(), entry.getValue());
                }
            }
            return new SpringCloudPropertySource(propertySource.getName(), values, ordinal);
        }
    }
}
