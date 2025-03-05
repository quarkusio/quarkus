package io.quarkus.it.oidc;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class SimpleBasicIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {
    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
            AuthenticationRequestContext authenticationRequestContext) {
        if ("Gaston".equals(request.getUsername()) && "Gaston".equals(new String(request.getPassword().getPassword()))) {
            return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal("Gaston"))
                    .setAnonymous(false)
                    .build());
        }
        return null;
    }
}
