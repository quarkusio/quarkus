package io.quarkus.elytron.security.jdbc.it;

import java.security.Permission;
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
        if ("worker".equals(identity.getPrincipal().getName())) {
            builder.addPermissionChecker(createWorkdayPermission());
        }
        return builder.build();
    }

    private Function<Permission, Uni<Boolean>> createStringPermission(String permissionName) {
        return new Function<Permission, Uni<Boolean>>() {
            @Override
            public Uni<Boolean> apply(Permission permission) {
                return Uni.createFrom().item(new StringPermission(permissionName).implies(permission));
            }
        };
    }

    private Function<Permission, Uni<Boolean>> createWorkdayPermission() {
        return new Function<Permission, Uni<Boolean>>() {
            @Override
            public Uni<Boolean> apply(Permission permission) {
                return Uni.createFrom().item(new WorkdayPermission("ignored", null, null).implies(permission));
            }
        };
    }

}
