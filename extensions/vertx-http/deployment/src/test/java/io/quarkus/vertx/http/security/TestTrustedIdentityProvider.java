package io.quarkus.vertx.http.security;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Singleton;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.TrustedAuthenticationRequest;

@Singleton
public class TestTrustedIdentityProvider implements IdentityProvider<TrustedAuthenticationRequest> {
    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public CompletionStage<SecurityIdentity> authenticate(TrustedAuthenticationRequest request,
            AuthenticationRequestContext context) {
        TestIdentityController.TestIdentity ident = TestIdentityController.idenitities.get(request.getPrincipal());
        if (ident == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture
                .completedFuture(QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal(request.getPrincipal()))
                        .addRoles(ident.roles).build());
    }
}
