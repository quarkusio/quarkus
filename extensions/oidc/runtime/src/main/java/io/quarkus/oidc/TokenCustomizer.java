package io.quarkus.oidc;

import jakarta.json.JsonObject;

/**
 * TokenCustomizer can be used to change token headers to their original value for the token verification to succeed.
 * Use it only if OIDC provider has changed some of the header values after the token signature has been created for
 * security reasons. Changing the headers in all other cases will lead to the token signature verification failure.
 * Please note that JSON canonicalization is not performed as part of JWT token signing process. It means that if OIDC
 * provider adds ignorable characters such as spaces or newline characters to JSON which represents token headers then
 * these characters will also be included as an additional input to the token signing process. In this case recreating
 * exactly the same JSON token headers sequence after the headers have been modified by this customizer will not be
 * possible and the signature verification will fail. Custom token customizers should be registered and discoverable as
 * CDI beans. They should be bound to specific OIDC tenants with a {@link TenantFeature} qualifier. with the exception
 * of named customizers provided by this extension which have to be selected with a `quarkus.oidc.token.customizer-name`
 * property. Custom token customizers without a {@link TenantFeature} qualifier will be bound to all OIDC tenants.
 */
public interface TokenCustomizer {
    /**
     * Customize token headers
     *
     * @param headers
     *        the token headers
     *
     * @return modified headers, null can be returned to indicate no modification has taken place
     */
    JsonObject customizeHeaders(JsonObject headers);
}
