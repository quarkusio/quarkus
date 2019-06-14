package io.quarkus.keycloak;

import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;

/**
 * An extension to Keycloak default {@code AdapterDeploymentContext} so that any additional requirement on how
 * {@link KeycloakDeployment}
 * instances are resolved.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class QuarkusDeploymentContext extends AdapterDeploymentContext {

    private KeycloakDeployment defaultDeployment;

    public QuarkusDeploymentContext() {
    }

    public QuarkusDeploymentContext(KeycloakDeployment defaultDeployment) {
        this.defaultDeployment = defaultDeployment;
    }

    void setConfigResolver(QuarkusKeycloakConfigResolver configResolver) {
        super.configResolver = configResolver;
        configResolver.init(defaultDeployment);
    }
}
