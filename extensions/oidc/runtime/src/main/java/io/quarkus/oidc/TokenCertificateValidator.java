package io.quarkus.oidc;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * TokenCertificateValidator can be used to verify X509 certificate chain
 * that is inlined in the JWT token as a 'x5c' header value.
 *
 * Use {@link TenantFeature} qualifier to bind this validator to specific OIDC tenants.
 */
public interface TokenCertificateValidator {
    /**
     * Validate X509 certificate chain
     *
     * @param oidcConfig current OIDC tenant configuration.
     * @param chain the certificate chain. The first element in the list is a leaf certificate, the last element - the root
     *        certificate.
     * @param tokenClaims the decoded JWT token claims in JSON format. If necessary, implementations can convert it to JSON
     *        object.
     * @throws {@link CertificateException} if the certificate chain validation has failed.
     */
    void validate(OidcTenantConfig oidcConfig, List<X509Certificate> chain, String tokenClaims) throws CertificateException;
}
