package io.quarkus.resteasy.reactive.server.test.security;

import java.security.Permission;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class BeanParamPermissionIdentityAugmentor implements SecurityIdentityAugmentor {

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
            AuthenticationRequestContext authenticationRequestContext) {
        var possessedPermission = createPossessedPermission(securityIdentity);
        var augmentedIdentity = QuarkusSecurityIdentity
                .builder(securityIdentity)
                .addPermissionChecker(requiredPermission -> Uni
                        .createFrom()
                        .item(requiredPermission.implies(possessedPermission)))
                .build();
        return Uni.createFrom().item(augmentedIdentity);
    }

    private Permission createPossessedPermission(SecurityIdentity securityIdentity) {
        // here comes your business logic
        return securityIdentity.isAnonymous() ? new StringPermission("list") : new StringPermission("read");
    }
}
