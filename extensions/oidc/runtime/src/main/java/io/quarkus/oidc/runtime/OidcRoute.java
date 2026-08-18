package io.quarkus.oidc.runtime;

import io.quarkus.oidc.OidcTenantConfig;

/**
 * Supported HTTP routes for OIDC tenant features that can be controlled with the
 * {@link io.quarkus.oidc.deployment.OidcBuildTimeConfig#allowedRoutes()}.
 */
public enum OidcRoute {
    /**
     * Allows tenants to use back-channel logout by accepting logout notifications from the OIDC provider.
     *
     * @see OidcTenantConfig.Backchannel
     */
    BACKCHANNEL_LOGOUT,
    /**
     * Allows tenants to publish protected resource metadata as defined by
     * <a href="https://datatracker.ietf.org/doc/rfc9728/">RFC 9728</a>.
     *
     * @see OidcTenantConfig.ResourceMetadata
     */
    RESOURCE_METADATA
}
