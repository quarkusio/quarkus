package io.quarkus.oidc.token.propagation.reactive.deployment.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class RolesSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    static final String SUPPORTED_USER = "alice";

    @Inject
    @RestClient
    RolesService rolesService;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
            AuthenticationRequestContext authenticationRequestContext) {
        if (securityIdentity != null && securityIdentity.getPrincipal() != null
                && SUPPORTED_USER.equals(securityIdentity.getPrincipal().getName())) {
            return rolesService
                    .getRole()
                    .map(role -> QuarkusSecurityIdentity.builder(securityIdentity).addRole(role).build());
        }
        return Uni.createFrom().item(securityIdentity);
    }
}
