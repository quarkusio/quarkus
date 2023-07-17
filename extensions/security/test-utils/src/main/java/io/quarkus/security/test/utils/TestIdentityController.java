package io.quarkus.security.test.utils;

import java.security.Permission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;

public class TestIdentityController {

    public static final Map<String, TestIdentity> identities = new ConcurrentHashMap<>();

    public static Builder resetRoles() {
        identities.clear();
        return new Builder();
    }

    public static class Builder {
        public Builder add(String username, String password) {
            identities.put(username, new TestIdentity(username, password, (String) null));
            return this;
        }

        public Builder add(String username, String password, String... roles) {
            identities.put(username, new TestIdentity(username, password, roles));
            return this;
        }

        public Builder add(String username, String password, Permission... permissions) {
            identities.put(username, new TestIdentity(username, password, permissions));
            return this;
        }
    }

    public static final class TestIdentity {

        public final String username;
        public final String password;
        public final Set<String> roles;
        public final List<Function<Permission, Uni<Boolean>>> permissionCheckers;

        private TestIdentity(String username, String password, String... roles) {
            this.username = username;
            this.password = password;
            this.roles = new HashSet<>(Arrays.asList(roles));
            this.permissionCheckers = List.of();
        }

        private TestIdentity(String username, String password, Permission... permissions) {
            this.username = username;
            this.password = password;
            this.roles = Set.of(username);
            this.permissionCheckers = createPermissionCheckers(Arrays.asList(permissions));
        }

        private static List<Function<Permission, Uni<Boolean>>> createPermissionCheckers(List<Permission> permissions) {
            return List.of(new Function<Permission, Uni<Boolean>>() {
                @Override
                public Uni<Boolean> apply(Permission requiredPermission) {
                    return Uni.createFrom().item(permissions.stream()
                            .anyMatch(possessedPermission -> possessedPermission.implies(requiredPermission)));
                }
            });
        }
    }
}
