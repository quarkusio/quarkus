package io.quarkus.it.keycloak;

import java.security.Principal;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context,
            Map<String, Object> attributes) {
        // assert RoutingContext is present in AuthenticationRequest attributes
        RoutingContext routingContext = HttpSecurityUtils.getRoutingContextAttribute(attributes);
        if (routingContext == null) {

            var container = Arc.container();
            if (!container.requestContext().isActive()
                    || container.instance(CurrentVertxRequest.class).get().getCurrent() != null) {
                // if Arc request context is active and RoutingContext is not available then there is no active
                // HTTP request => AKA this is an event bus scenario and the RoutingContext cannot be available
                throw new IllegalStateException("RoutingContext is missing in AuthenticationRequest attributes");
            }
        }

        return augment(identity, context);
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        RoutingContext routingContext = identity.getAttribute(RoutingContext.class.getName());
        if (routingContext != null &&
                (routingContext.normalizedPath().endsWith("code-flow-user-info-only")
                        || routingContext.normalizedPath().endsWith("code-flow-user-info-github")
                        || routingContext.normalizedPath().endsWith("code-flow-user-info-dynamic-github")
                        || routingContext.normalizedPath().endsWith("code-flow-token-introspection")
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
