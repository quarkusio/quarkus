package io.quarkus.vertx.http.security;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Singleton;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.smallrye.mutiny.Uni;

@Singleton
public class TestTrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {
    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request,
            AuthenticationRequestContext context) {
        TestIdentityController.TestIdentity ident = TestIdentityController.idenitities.get(request.getPrincipal());
        if (ident == null) {
            return Uni.createFrom().optional(Optional.empty());
        }
        return Uni.createFrom().completionStage(CompletableFuture
                .completedFuture(QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal(request.getPrincipal()))
                        .addRoles(ident.roles).build()));
    }
}
