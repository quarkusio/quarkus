package io.quarkus.elytron.security.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.elytron.security.runtime.AuthConfig;
import io.quarkus.elytron.security.runtime.SecurityRecorder;
import io.quarkus.runtime.RuntimeValue;
import io.undertow.security.idm.IdentityManager;

/**
 * Information about an authentication mechanism to enable. This is used to call the
 * {@linkplain SecurityRecorder#configureUndertowIdentityManager(RuntimeValue, IdentityManager, List)}
 * method to register the auth methods enabled.
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
