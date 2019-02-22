package io.quarkus.security;

import org.jboss.builder.item.MultiBuildItem;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.runtime.RuntimeValue;

/**
 * A build item for the {@linkplain SecurityRealm} runtime values created for the deployment. These are combined into a
 * single {@linkplain org.wildfly.security.auth.server.SecurityDomain} by the {@linkplain SecurityDeploymentProcessor}.
 */
public final class SecurityRealmBuildItem extends MultiBuildItem {
    private final RuntimeValue<SecurityRealm> realm;
    private final AuthConfig authConfig;

    public SecurityRealmBuildItem(RuntimeValue<SecurityRealm> realm, AuthConfig authConfig) {
        this.realm = realm;
        this.authConfig = authConfig;
    }

    public RuntimeValue<SecurityRealm> getRealm() {
        return realm;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }
}
