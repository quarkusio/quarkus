package io.quarkus.spring.cloud.config.client.runtime;

import static java.lang.String.join;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.util.StringUtil;
import io.quarkus.spring.cloud.config.client.runtime.Response.PropertySource;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory.ConfigurableConfigSourceFactory;
import io.smallrye.config.common.MapBackedConfigSource;

public class SpringCloudConfigClientConfigSourceFactory
        implements ConfigurableConfigSourceFactory<SpringCloudConfigClientConfig> {
    private static final Logger log = Logger.getLogger(SpringCloudConfigClientConfigSourceFactory.class);

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context,
            final SpringCloudConfigClientConfig config) {
        boolean inAppCDsGeneration = Boolean
                .parseBoolean(System.getProperty(ApplicationLifecycleManager.QUARKUS_APPCDS_GENERATE_PROP, "false"));
        if (inAppCDsGeneration) {
            return Collections.emptyList();
        }

        List<ConfigSource> sources = new ArrayList<>();

        if (!config.enabled()) {
            log.debug(
                    "No attempt will be made to obtain configuration from the Spring Cloud Config Server because the functionality has been disabled via configuration");
            return sources;
        }

        String applicationName = config.name();
        if (StringUtil.isNullOrEmpty(applicationName)) {
            log.warn(
                    "No attempt will be made to obtain configuration from the Spring Cloud Config Server because the application name has not been set. Consider setting it via 'quarkus.spring-cloud-config.name'");
            return sources;
        }

        boolean connectionTimeoutIsGreaterThanZero = !config.connectionTimeout().isNegative()
                && !config.connectionTimeout().isZero();
        boolean readTimeoutIsGreaterThanZero = !config.readTimeout().isNegative() && !config.readTimeout().isZero();

        VertxSpringCloudConfigGateway client = new VertxSpringCloudConfigGateway(config);
        try {
            String profiles = determineProfiles(context, config);
            log.debug("The following profiles will be used to look up properties: " + profiles);
            Response response;
            if (connectionTimeoutIsGreaterThanZero || readTimeoutIsGreaterThanZero) {
                response = client.exchange(applicationName, profiles).await()
                        .atMost(config.connectionTimeout().plus(config.readTimeout().multipliedBy(2)));
            } else {
                response = client.exchange(applicationName, profiles).await().indefinitely();
            }

            List<PropertySource> propertySources = response.getPropertySources();
            // Profiles are looked from the highest ordinal to lowest, so we reverse the collection to build the source list
            Collections.reverse(propertySources);

            for (int i = 0, propertySourcesSize = propertySources.size(); i < propertySourcesSize; i++) {
                int ordinal = config.ordinal() + i;
                PropertySource propertySource = propertySources.get(i);
                if (log.isDebugEnabled()) {
                    log.debug("Adding PropertySource named '" + propertySource.getName() + "', with and ordinal of '" + ordinal
                            + "' that contains the following keys: " + join(",", propertySource.getSource().keySet()));
                }
                sources.add(new MapBackedConfigSource(propertySource.getName(), propertySource.getSource(), ordinal) {
                });
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

    private static String determineProfiles(ConfigSourceContext context, SpringCloudConfigClientConfig config) {
        if (config.profiles().isPresent()) {
            return join(",", config.profiles().get());
        }
        // The profile list starts with the profile overrides, but setting the property is done backwards
        List<String> profiles = new ArrayList<>(context.getProfiles());
        Collections.reverse(profiles);
        return join(",", profiles);
    }
}
