package io.quarkus.restclient.config.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.restclient.config.AbstractRestClientConfigBuilder;
import io.quarkus.restclient.config.RegisteredRestClient;
import io.quarkus.restclient.config.RestClientKeysProvider;
import io.quarkus.restclient.config.RestClientsBuildTimeConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

/**
 * Provides a new {@link RestClientsBuildTimeConfig} with the discovered registered REST Clients configuration
 * only. This should be preferred once REST Clients are discovered and validated to keep only the required
 * configuration.
 * <p>
 * This has to be done manually, because the {@link RestClientsBuildTimeConfig} is marked for
 * {@link io.quarkus.runtime.annotations.ConfigPhase#BUILD_TIME}, and the REST Clients are not known when the
 * configuration starts (before build steps execution).
 *
 * @see io.quarkus.restclient.config.AbstractRestClientConfigBuilder
 */
public final class RestClientsBuildTimeConfigBuildItem extends SimpleBuildItem {
    private final List<RegisteredRestClient> restClients;
    private final SmallRyeConfig config;
    private final RestClientsBuildTimeConfig restClientsBuildTimeConfig;

    public RestClientsBuildTimeConfigBuildItem(final List<RegisteredRestClient> restClients) {
        this.restClients = Collections.unmodifiableList(restClients);
        this.config = new SmallRyeConfigBuilder()
                .withSources(new ConfigSource() {
                    final SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);

                    @Override
                    public Set<String> getPropertyNames() {
                        Set<String> properties = new HashSet<>();
                        config.getPropertyNames().forEach(properties::add);
                        return properties;
                    }

                    @Override
                    public String getValue(final String propertyName) {
                        ConfigValue configValue = config.getConfigValue(propertyName);
                        if (configValue.getValue() != null && !configValue.isDefault()) {
                            return configValue.getValue();
                        }
                        return null;
                    }

                    @Override
                    public String getName() {
                        return "SmallRye Config";
                    }
                })
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
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
        this.restClientsBuildTimeConfig = config.getConfigMapping(RestClientsBuildTimeConfig.class);
        RestClientKeysProvider.KEYS.clear();
    }

    public List<RegisteredRestClient> getRestClients() {
        return restClients;
    }

    public SmallRyeConfig getConfig() {
        return config;
    }

    public RestClientsBuildTimeConfig getRestClientsBuildTimeConfig() {
        return restClientsBuildTimeConfig;
    }

    public Optional<BuiltinScope> getScope(final Capabilities capabilities, final ClassInfo restClientInterface) {
        List<Optional<BuiltinScope>> discoveredScopes = new ArrayList<>();

        // First config in the rest client service
        restClientsBuildTimeConfig.clients().get(restClientInterface.name().toString()).scope()
                .ifPresent(s -> discoveredScopes
                        .add(builtinScopeFromName(DotName.createSimple(s), restClientInterface.name().toString())));

        // Second annotation in the rest client declaration
        Set<DotName> annotations = restClientInterface.annotationsMap().keySet();
        for (DotName annotationName : annotations) {
            BuiltinScope builtinScope = BuiltinScope.from(annotationName);
            if (builtinScope != null) {
                discoveredScopes.add(Optional.of(builtinScope));
            }
        }

        // Third global config
        restClientsBuildTimeConfig.scope()
                .ifPresent(s -> discoveredScopes
                        .add(builtinScopeFromName(DotName.createSimple(s), restClientInterface.name().toString())));

        Optional<BuiltinScope> scope = discoveredScopes.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
        if (scope.isPresent()) {
            if (scope.get().equals(BuiltinScope.SESSION)) {
                if (capabilities.isPresent(Capability.SERVLET)) {
                    return scope;
                }
            } else {
                return scope;
            }
        }
        return Optional.empty();
    }

    private static Optional<BuiltinScope> builtinScopeFromName(DotName scopeName, String restClientClass) {
        BuiltinScope scope = BuiltinScope.from(scopeName);
        if (scope != null) {
            return Optional.of(scope);
        }

        for (BuiltinScope builtinScope : BuiltinScope.values()) {
            if (builtinScope.getName().withoutPackagePrefix().equalsIgnoreCase(scopeName.toString())) {
                return Optional.of(builtinScope);
            }
        }
        throw new ConfigurationException(
                String.format("Not possible to define the scope %s for the REST client %s ", scopeName, restClientClass));
    }
}
