package io.quarkus.devservices.keycloak;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.representations.idm.RealmRepresentation;

public interface KeycloakDevServicesConfigurator {

    record ConfigPropertiesContext(
            String authServerInternalUrl,
            String oidcClientId,
            String oidcClientSecret,
            String authServerInternalBaseUrl,
            String keycloakUrl,
            String clientAuthServerUrl,
            String oidcUsers,
            String keycloakRealms) {

        public ConfigPropertiesContext(
                String authServerInternalUrl,
                String oidcClientId,
                String oidcClientSecret,
                String authServerInternalBaseUrl,
                String keycloakUrl,
                String clientAuthServerUrl,
                Map<String, String> oidcUsers,
                Collection<String> keycloakRealms) {
            this(
                    authServerInternalUrl,
                    oidcClientId,
                    oidcClientSecret,
                    authServerInternalBaseUrl,
                    keycloakUrl,
                    clientAuthServerUrl,
                    oidcUsers.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(",")),
                    String.join(",", keycloakRealms));
        }

    }

    Set<String> getLazyConfigKeys();

    String getLazyConfigValue(String configKey, ConfigPropertiesContext context);

    default void customizeDefaultRealm(RealmRepresentation realmRepresentation) {
    }

    record LazyConfigProperty(String configKey, Function<ConfigPropertiesContext, String> ctxToConfigValue) {

        public LazyConfigProperty(String configKey, String configValue) {
            this(configKey, ctx -> configValue);
        }

    }

    record LazyConfigKeycloakDevServicesConfigurator(
            Collection<LazyConfigProperty> lazyConfigProperties) implements KeycloakDevServicesConfigurator {
        @Override
        public Set<String> getLazyConfigKeys() {
            return lazyConfigProperties.stream().map(p -> p.configKey).collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public String getLazyConfigValue(String configKey, ConfigPropertiesContext context) {
            return lazyConfigProperties.stream()
                    .filter(p -> p.configKey.equals(configKey))
                    .map(p -> p.ctxToConfigValue.apply(context))
                    .filter(Objects::nonNull)
                    .filter(Predicate.not(String::isEmpty))
                    .findFirst()
                    .orElse("");
        }
    }

    record ComposedKeycloakDevServicesConfigurator(
            Collection<KeycloakDevServicesConfigurator> configurators) implements KeycloakDevServicesConfigurator {
        @Override
        public Set<String> getLazyConfigKeys() {
            return configurators.stream()
                    .flatMap(c -> c.getLazyConfigKeys().stream())
                    .collect(Collectors.toUnmodifiableSet());
        }

        @Override
        public String getLazyConfigValue(String configKey, ConfigPropertiesContext context) {
            return configurators.stream()
                    .filter(c -> c.getLazyConfigKeys().contains(configKey))
                    .map(c -> c.getLazyConfigValue(configKey, context))
                    .filter(Objects::nonNull)
                    .filter(Predicate.not(String::isEmpty))
                    .findFirst()
                    .orElse("");
        }

        @Override
        public void customizeDefaultRealm(RealmRepresentation realmRepresentation) {
            configurators.forEach(c -> c.customizeDefaultRealm(realmRepresentation));
        }
    }
}
