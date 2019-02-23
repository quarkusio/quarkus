package io.quarkus.security;

import java.util.List;

import org.jboss.builder.item.MultiBuildItem;

import io.quarkus.runtime.RuntimeValue;

/**
 * Information about an authentication mechanism to enable. This is used to call the
 * {@linkplain SecurityTemplate#configureUndertowIdentityManager(RuntimeValue, List)} method to register the auth methods
 * enabled.
 *
 * Custom authentication mechanisms would need to have the {@linkplain io.undertow.servlet.ServletExtension} that
 * registers the {@linkplain io.undertow.security.api.AuthenticationMechanismFactory} for the method.
 */
public final class AuthConfigBuildItem extends MultiBuildItem {
    private final AuthConfig authConfig;

    public AuthConfigBuildItem(AuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }
}
