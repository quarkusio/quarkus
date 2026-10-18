package io.quarkus.oidc.client.registration;

import java.io.Closeable;
import java.util.Map;

import io.smallrye.mutiny.Uni;

/**
 * OIDC client registrations
 */
public interface OidcClientRegistrations extends Closeable {

    /**
     * Default OIDC client registration.
     *
     * @return {@link OidcClientRegistration}, null if no OIDC client registration configuration is available.
     */
    OidcClientRegistration getClientRegistration();

    /**
     * Return a named OIDC client registration
     *
     * @param id OIDC client registration id.
     * @return {@link OidcClientRegistration}, null if no named OIDC client registration configuration is available.
     */
    OidcClientRegistration getClientRegistration(String id);

    /**
     * Return a map of all OIDC client registrations created from configured OIDC client registration configurations.
     *
     * @return Map of OIDC client registrations
     */
    Map<String, OidcClientRegistration> getClientRegistrations();

    /**
     * Create a new OIDC client registration
     *
     * @param oidcConfig OIDC client registration configuration
     * @return Uni<OidcClientRegistration>
     */
    Uni<OidcClientRegistration> newClientRegistration(OidcClientRegistrationConfig oidcConfig);
}
