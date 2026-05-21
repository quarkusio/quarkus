package io.quarkus.email.authentication.runtime.internal;

import io.quarkus.email.authentication.EmailAuthenticationRequest;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

final class EmailAuthenticationIdentityProvider implements IdentityProvider<EmailAuthenticationRequest> {

    @Override
    public Class<EmailAuthenticationRequest> getRequestType() {
        return EmailAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(EmailAuthenticationRequest req, AuthenticationRequestContext reqCtx) {
        return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(req.getEmailAddress()))
                .build());
    }
}
