package io.quarkus.security.runtime;

import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import jakarta.inject.Singleton;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.CertificateAuthenticationRequest;
import io.smallrye.mutiny.Uni;

@Singleton
public class X509IdentityProvider implements IdentityProvider<CertificateAuthenticationRequest> {
    private static final String COMMON_NAME = "CN";
    private static final String ROLES_ATTRIBUTE = "roles";

    @Override
    public Class<CertificateAuthenticationRequest> getRequestType() {
        return CertificateAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(CertificateAuthenticationRequest request, AuthenticationRequestContext context) {
        X509Certificate certificate = request.getCertificate().getCertificate();
        Map<String, Set<String>> roles = request.getAttribute(ROLES_ATTRIBUTE);
        return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                .setPrincipal(certificate.getSubjectX500Principal())
                .addCredential(request.getCertificate())
                .addRoles(extractRoles(certificate, roles))
                .build());
    }

    private Set<String> extractRoles(X509Certificate certificate, Map<String, Set<String>> roles) {
        if (roles == null) {
            return Set.of();
        }
        X500Principal principal = certificate.getSubjectX500Principal();
        if (principal == null || principal.getName() == null) {
            return Set.of();
        }
        Set<String> matchedRoles = roles.get(principal.getName());
        if (matchedRoles != null) {
            return matchedRoles;
        }
        String commonName = getCommonName(principal);
        if (commonName != null) {
            matchedRoles = roles.get(commonName);
            if (matchedRoles != null) {
                return matchedRoles;
            }
        }
        return Set.of();
    }

    private static String getCommonName(X500Principal principal) {
        try {
            LdapName ldapDN = new LdapName(principal.getName());

            // Apparently for some CN variations it might not produce correct results
            // Can be tuned as necessary.
            for (Rdn rdn : ldapDN.getRdns()) {
                if (COMMON_NAME.equals(rdn.getType())) {
                    return rdn.getValue().toString();
                }
            }
        } catch (InvalidNameException ex) {
            // Failing the augmentation process because of this exception seems unnecessary
            // The common name my include some characters unexpected by the legacy LdapName API specification.
        }
        return null;
    }
}
