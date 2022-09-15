package io.quarkus.it.keycloak;

import java.security.Principal;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        RoutingContext routingContext = identity.getAttribute(RoutingContext.class.getName());
        if (routingContext != null &&
                (routingContext.normalizedPath().endsWith("code-flow-user-info-only")
                        || routingContext.normalizedPath().endsWith("code-flow-user-info-github")
                        || routingContext.normalizedPath().endsWith("code-flow-user-info-dynamic-github")
                        || routingContext.normalizedPath().endsWith("code-flow-user-info-github-cached-in-idtoken"))) {
            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
            UserInfo userInfo = identity.getAttribute("userinfo");
            builder.setPrincipal(new Principal() {

                @Override
                public String getName() {
                    return userInfo.getString("preferred_username");
                }

            });
            identity = builder.build();
        }
        return Uni.createFrom().item(identity);
    }

}
