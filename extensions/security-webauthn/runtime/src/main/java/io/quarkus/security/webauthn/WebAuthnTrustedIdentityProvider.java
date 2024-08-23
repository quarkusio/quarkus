package io.quarkus.security.webauthn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * IdentityProvider for {@link TrustedAuthenticationRequest}
 */
@ApplicationScoped
public class WebAuthnTrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {

    @Inject
    WebAuthnSecurity security;

    @Inject
    WebAuthnUserProvider userProvider;

    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request, AuthenticationRequestContext context) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
        builder.setPrincipal(new QuarkusPrincipal(request.getPrincipal()));
        builder.addRoles(userProvider.getRoles(request.getPrincipal()));
        return Uni.createFrom().item(builder.build());
    }
}
