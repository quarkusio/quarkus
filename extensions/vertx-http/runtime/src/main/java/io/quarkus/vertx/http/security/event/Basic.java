package io.quarkus.vertx.http.security.event;

/**
 * A CDI event that facilitates programmatic setup of the Basic authentication.
 */
public interface Basic {

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
     */
    Basic enable();

    /**
     * Set an authentication realm instead of the realm configured with the 'quarkus.http.auth.realm' configuration property.
     */
    Basic realm(String realm);

}
