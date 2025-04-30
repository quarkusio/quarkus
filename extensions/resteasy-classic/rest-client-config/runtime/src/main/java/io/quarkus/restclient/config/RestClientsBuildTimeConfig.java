package io.quarkus.restclient.config;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.rest-client")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface RestClientsBuildTimeConfig {
    /**
     * Configurations of REST client instances.
     */
    @WithParentName
    @WithDefaults
    Map<String, RestClientBuildConfig> clients();

    /**
     * If true, the extension will automatically remove the trailing slash in the paths if any.
     * This property is not applicable to the RESTEasy Client.
     */
    @WithName("removes-trailing-slash")
    @WithDefault("true")
    boolean removesTrailingSlash();

    interface RestClientBuildConfig {

        /**
         * The CDI scope to use for injection. This property can contain either a fully qualified class name of a CDI scope
         * annotation (such as "jakarta.enterprise.context.ApplicationScoped") or its simple name (such as
         * "ApplicationScoped").
         * By default, this is not set which means the interface is not registered as a bean unless it is annotated with
         * {@link RegisterRestClient}.
         * If an interface is not annotated with {@link RegisterRestClient} and this property is set, then Quarkus will make the
         * interface
         * a bean of the configured scope.
         */
        Optional<String> scope();

        /**
         * If set to true, then Quarkus will ensure that all calls from the REST client go through a local proxy
         * server (that is managed by Quarkus).
         * This can be very useful for capturing network traffic to a service that uses HTTPS.
         * <p>
         * This property is not applicable to the RESTEasy Client, only the Quarkus REST client (formerly RESTEasy Reactive
         * client).
         * <p>
         * This property only applicable to dev and test mode.
         */
        @WithDefault("false")
        boolean enableLocalProxy();

        /**
         * This setting is used to select which proxy provider to use if there are multiple ones.
         * It only applies if {@code enable-local-proxy} is true.
         * <p>
         * The algorithm for picking between multiple provider is the following:
         * <ul>
         * <li>If only the default is around, use it (its name is {@code default})</li>
         * <li>If there is only one besides the default, use it</li>
         * <li>If there are multiple ones, fail</li>
         * </ul>
         */
        Optional<String> localProxyProvider();

        /**
         * If true, the extension will automatically remove the trailing slash in the paths if any.
         * This property is not applicable to the RESTEasy Client.
         */
        @WithName("removes-trailing-slash")
        @WithDefault("true")
        boolean removesTrailingSlash();
    }

    /**
     * Provides a new {@link RestClientsBuildTimeConfig} with the discovered registered REST Clients configuration
     * only. This should be preferred once REST Clients are discovered and validated to keep only the required
     * configuration.
     * <p>
     * This has to be done manually, because the {@link RestClientsBuildTimeConfig} is marked for
     * {@link ConfigPhase#BUILD_TIME}, and the REST Clients are not known when the configuration starts.
     *
     * @param restClients the discovered registered REST Clients.
     * @return a {@link RestClientsBuildTimeConfig} with the discovered registered REST Clients configuration only.
     */
    default RestClientsBuildTimeConfig get(List<RegisteredRestClient> restClients) {
        return getConfig(restClients).getConfigMapping(RestClientsBuildTimeConfig.class);
    }

    default SmallRyeConfig getConfig(List<RegisteredRestClient> restClients) {
        return new SmallRyeConfigBuilder()
                .withSources(
                        new ConfigSource() {
                            final SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
                            final ConfigSource defaultsSource = getDefaultsSource();

                            @Override
                            public Set<String> getPropertyNames() {
                                Set<String> properties = new HashSet<>();
                                config.getPropertyNames().forEach(properties::add);
                                return properties;
                            }

                            @Override
                            public String getValue(final String propertyName) {
                                ConfigValue configValue = config.getConfigValue(propertyName);
                                if (configValue != null && !defaultsSource.getName().equals(configValue.getSourceName())) {
                                    return configValue.getValue();
                                }
                                return null;
                            }

                            @Override
                            public String getName() {
                                return "SmallRye Config";
                            }

                            private ConfigSource getDefaultsSource() {
                                ConfigSource configSource = null;
                                for (ConfigSource source : config.getConfigSources()) {
                                    configSource = source;
                                }
                                return configSource;
                            }
                        })
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder(false) {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return restClients;
                            }
                        }.configBuilder(builder);
                    }
                })
                .withMapping(RestClientsBuildTimeConfig.class)
                .withMappingIgnore("quarkus.**")
                .build();
    }
}
