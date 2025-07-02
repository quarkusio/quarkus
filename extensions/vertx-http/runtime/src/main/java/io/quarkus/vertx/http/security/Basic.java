package io.quarkus.vertx.http.security;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Represents a Basic authentication configuration.
 */
public record Basic(String realm, Optional<Boolean> enabled) {

    /**
     * Enables the Basic authentication during the runtime if it wasn't already explicitly enabled with
     * the 'quarkus.http.auth.basic' configuration property during the build-time. This may not be necessary
     * if the Basic authentication is implicitly enabled by Quarkus Security when:
     *
     * <ul>
     * <li>No custom {@link io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism} is detected
     * and at least one HTTP permission requires basic authentication.</li>
     * <li>An endpoint is annotated with the {@link io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication}
     * annotation.</li>
     * </ul>
     *
     * If you require that the {@link io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism} can be injected
     * as a CDI bean or use SmallRye OpenAPI, use the 'quarkus.http.auth.basic' configuration property instead.
     * <p>
     * This method also configures the authentication realm to the 'quarkus.http.auth.realm' configuration property value.
     * </p>
     */
    public static Basic enable() {
        return new Basic(ConfigProvider.getConfig().getOptionalValue("quarkus.http.auth.realm", String.class).orElse(null),
                Optional.of(true));
    }

    /**
     * Enables the Basic authentication and sets an authentication realm instead of the realm configured
     * with the 'quarkus.http.auth.realm' configuration property.
     *
     * @see #enable()
     */
    public static Basic realm(String authenticationRealm) {
        return new Basic(authenticationRealm, Optional.of(true));
    }

}
