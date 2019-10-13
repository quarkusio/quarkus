package io.quarkus.vertx.http.security;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;

@ApplicationScoped
public class TestIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public CompletionStage<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
            AuthenticationRequestContext context) {
        TestIdentityController.TestIdentity ident = TestIdentityController.idenitities.get(request.getUsername());
        if (ident == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (!ident.password.equals(new String(request.getPassword().getPassword()))) {
            CompletableFuture<SecurityIdentity> ret = new CompletableFuture<>();
            ret.completeExceptionally(new AuthenticationFailedException());
            return ret;
        }
        QuarkusSecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(ident.username))
                .addRoles(ident.roles)
                .addCredential(request.getPassword())
                .build();
        return CompletableFuture.completedFuture(identity);
    }

}
