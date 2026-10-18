package io.quarkus.it.mongodb.rest.data.panache;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class TestSecurityIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(
            UsernamePasswordAuthenticationRequest authenticationRequest,
            AuthenticationRequestContext authenticationRequestContext) {
        if (authenticationRequest.getUsername().equals("joe")
                && "doe".equals(new String(authenticationRequest.getPassword().getPassword()))) {
            return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal("joe"))
                    .addPermissionAsString("get-author")
                    .setAnonymous(false)
                    .build());
        }
        return Uni.createFrom().nullItem();
    }

}
