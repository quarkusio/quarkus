package io.quarkus.resteasy.reactive.server.test.security;

import java.security.Permission;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.StringPermission;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class PermissionsIdentityAugmentor implements SecurityIdentityAugmentor {

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }
        return Uni.createFrom().item(build(identity));
    }

    SecurityIdentity build(SecurityIdentity identity) {
        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder(identity);
        switch (identity.getPrincipal().getName()) {
            case "admin":
                builder.addPermissionChecker(new PermissionCheckBuilder().addPermission("update").addPermission("create")
                        .addPermission("read", "resource-admin").addCustomPermission().build());
                break;
            case "user":
                builder.addPermissionChecker(new PermissionCheckBuilder().addPermission("update").addPermission("get-identity")
                        .addPermission("read", "resource-admin").build());
                break;
            case "viewer":
                builder.addPermissionChecker(new PermissionCheckBuilder().addPermission("read", "resource-viewer").build());
                break;
        }
        return builder.build();
    }

    private static final class PermissionCheckBuilder {

        private final Set<Permission> permissionSet = new HashSet<>();

        PermissionCheckBuilder addPermission(String name, String action) {
            permissionSet.add(new StringPermission(name, action));
            return this;
        }

        PermissionCheckBuilder addPermission(String name) {
            permissionSet.add(new StringPermission(name));
            return this;
        }

        PermissionCheckBuilder addCustomPermission() {
            permissionSet.add(new CustomPermission("ignored"));
            return this;
        }

        Function<Permission, Uni<Boolean>> build() {
            final var immutablePermissions = Set.copyOf(permissionSet);
            return new Function<Permission, Uni<Boolean>>() {
                @Override
                public Uni<Boolean> apply(Permission requiredPermission) {
                    return Uni.createFrom().item(immutablePermissions.stream()
                            .anyMatch(possessedPermission -> possessedPermission.implies(requiredPermission)));
                }
            };
        }
    }

}
