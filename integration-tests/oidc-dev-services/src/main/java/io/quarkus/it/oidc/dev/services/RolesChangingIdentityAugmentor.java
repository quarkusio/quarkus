package io.quarkus.it.oidc.dev.services;

import java.security.Permission;
import java.util.Map;
import java.util.function.Function;

import jakarta.inject.Singleton;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public final class RolesChangingIdentityAugmentor implements SecurityIdentityAugmentor {

    private volatile int invocationCount = 0;

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context,
            Map<String, Object> attributes) {
        if (shouldNotAugment(attributes)) {
            return Uni.createFrom().item(identity);
        }
        // drop previous identity roles and use 'user' role instead
        return Uni.createFrom().item(QuarkusSecurityIdentity
                .builder()
                .addAttributes(identity.getAttributes())
                .addCredentials(identity.getCredentials())
                .addPermissionChecker(new Function<Permission, Uni<Boolean>>() {
                    @Override
                    public Uni<Boolean> apply(Permission permission) {
                        return identity.checkPermission(permission);
                    }
                })
                .addRole("user")
                .setPrincipal(identity.getPrincipal())
                .setAnonymous(false)
                .build());
    }

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity securityIdentity,
            AuthenticationRequestContext authenticationRequestContext) {
        throw new IllegalStateException();
    }

    private boolean shouldNotAugment(Map<String, Object> attributes) {
        RoutingContext routingContext = HttpSecurityUtils.getRoutingContextAttribute(attributes);
        if (routingContext == null) {
            return true;
        }
        if (!routingContext.normalizedPath().contains("/change-in-updated-identity-roles")) {
            return true;
        }
        invocationCount++;
        boolean firstInvocation = invocationCount == 1;
        return firstInvocation;
    }
}
