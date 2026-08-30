package io.quarkus.oidc.runtime;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig;

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
    RESOURCE_METADATA,
    /**
     * Allows self-attesting tenants to publish an attestation JSON Web Key Set (JWKS) used to verify
     * client attestation JWTs as defined by
     * <a href="https://datatracker.ietf.org/doc/draft-ietf-oauth-attestation-based-client-auth/">OAuth 2.0
     * Attestation-Based Client Authentication</a>.
     *
     * @see OidcClientCommonConfig.Credentials.Attestation
     */
    CLIENT_ATTESTATION
}
