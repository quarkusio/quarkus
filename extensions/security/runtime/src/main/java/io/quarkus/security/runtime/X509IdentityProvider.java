package io.quarkus.security.runtime;

import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.function.Function;

import jakarta.inject.Singleton;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.CertificateAuthenticationRequest;
import io.smallrye.mutiny.Uni;

@Singleton
public class X509IdentityProvider implements IdentityProvider<CertificateAuthenticationRequest> {
    private static final String ROLES_MAPPER_ATTRIBUTE = "roles_mapper";

    @Override
    public Class<CertificateAuthenticationRequest> getRequestType() {
        return CertificateAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(CertificateAuthenticationRequest request, AuthenticationRequestContext context) {
        X509Certificate certificate = request.getCertificate().getCertificate();
        return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                .setPrincipal(certificate.getSubjectX500Principal())
                .addCredential(request.getCertificate())
                .addRoles(extractRoles(certificate, request.getAttribute(ROLES_MAPPER_ATTRIBUTE)))
                .build());
    }

    private static Set<String> extractRoles(X509Certificate certificate,
            Function<X509Certificate, Set<String>> certificateToRoles) {
        return certificateToRoles == null ? Set.of() : certificateToRoles.apply(certificate);
    }
}
