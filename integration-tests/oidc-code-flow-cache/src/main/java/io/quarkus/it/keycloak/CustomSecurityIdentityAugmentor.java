package io.quarkus.it.keycloak;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdTokenCredential;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        IdTokenCredential cred = identity.getCredential(IdTokenCredential.class);
        if (cred != null) {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
            builder.addAttribute(RoutingContext.class.getName(), cred.getRoutingContext());
            builder.addAttribute(OidcConstants.ACCESS_TOKEN_VALUE,
                    identity.getCredential(AccessTokenCredential.class).getToken());
            identity = builder.build();
        }
        return Uni.createFrom().item(identity);
    }

}
