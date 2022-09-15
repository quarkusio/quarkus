package io.quarkus.security.test.utils;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class TestIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
            AuthenticationRequestContext context) {
        TestIdentityController.TestIdentity ident = TestIdentityController.identities.get(request.getUsername());
        if (ident == null) {
            return Uni.createFrom().optional(Optional.empty());
        }
        if (!ident.password.equals(new String(request.getPassword().getPassword()))) {
            return Uni.createFrom().failure(new AuthenticationFailedException());
        }
        QuarkusSecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(ident.username))
                .addRoles(ident.roles)
                .addCredential(request.getPassword())
                .build();
        return Uni.createFrom().item(identity);
    }

}
