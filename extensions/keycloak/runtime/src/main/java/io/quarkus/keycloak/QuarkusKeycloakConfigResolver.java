package io.quarkus.keycloak;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * <p>
 * A {@link KeycloakConfigResolver} that is installed to applications so that any custom {@code KeycloakConfigResolver} instance
 * produced by applications are considered when resolving {@link KeycloakDeployment} instances.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class QuarkusKeycloakConfigResolver implements KeycloakConfigResolver {

    @Inject
    Instance<KeycloakConfigResolver> configResolvers;

    private KeycloakConfigResolver delegate;
    private KeycloakDeployment defaultDeployment;

    @Override
    public KeycloakDeployment resolve(HttpFacade.Request facade) {
        if (delegate == null) {
            return defaultDeployment;
        }

        KeycloakDeployment deployment = delegate.resolve(facade);

        if (deployment == null) {
            deployment = defaultDeployment;
        }

        return deployment;
    }

    void init(KeycloakDeployment defaultDeployment) {
        this.defaultDeployment = defaultDeployment;
        delegate = configResolvers.stream()
                .filter(keycloakConfigResolver -> !QuarkusKeycloakConfigResolver.class.isInstance(keycloakConfigResolver))
                .findAny().orElse(null);
    }
}
