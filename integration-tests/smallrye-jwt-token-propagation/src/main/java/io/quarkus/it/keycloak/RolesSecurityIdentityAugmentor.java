package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class RolesSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    public static final String USE_SEC_IDENTITY_AUGMENTOR = RolesSecurityIdentityAugmentor.class.getName();

    @Inject
    @RestClient
    RolesService rolesService;

    @Inject
    RoutingContext routingContext;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
            AuthenticationRequestContext authenticationRequestContext) {
        if (securityIdentity != null && securityIdentity.getPrincipal() != null
                && "alice".equals(securityIdentity.getPrincipal().getName())) {
            boolean augmentIdentity = Boolean.parseBoolean(routingContext.request().getHeader(USE_SEC_IDENTITY_AUGMENTOR));
            if (augmentIdentity) {
                return authenticationRequestContext.runBlocking(() -> {
                    String role = rolesService.getRole();
                    return QuarkusSecurityIdentity.builder(securityIdentity).addRole(role).build();
                });
            }
        }
        return Uni.createFrom().item(securityIdentity);
    }
}
