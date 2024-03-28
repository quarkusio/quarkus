package io.quarkus.vertx.http.runtime.security;

import java.security.Permission;
import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

public class RolesMapping implements Function<SecurityIdentity, SecurityIdentity> {

    static final String ROLES_MAPPING_KEY = "io.quarkus.vertx.http.runtime.security.RolesMapping";
    private final Map<String, Set<Permission>> roleToPermissions;
    private final Map<String, List<String>> roleToRoles;
    protected final boolean grantPermissions;
    protected final boolean grantRoles;

    RolesMapping(Map<String, Set<Permission>> roleToPermissions,
            Map<String, List<String>> roleToRoles) {
        if (roleToPermissions != null && !roleToPermissions.isEmpty()) {
            this.grantPermissions = true;
            this.roleToPermissions = Map.copyOf(roleToPermissions);
        } else {
            this.grantPermissions = false;
            this.roleToPermissions = null;
        }
        if (roleToRoles != null && !roleToRoles.isEmpty()) {
            this.grantRoles = true;
            this.roleToRoles = Map.copyOf(roleToRoles);
        } else {
            this.grantRoles = false;
            this.roleToRoles = null;
        }
    }

    public static RolesMapping of(Map<String, List<String>> roleToRoles) {
        return roleToRoles.isEmpty() ? null : new RolesMapping(null, roleToRoles);
    }

    @Override
    public SecurityIdentity apply(SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            return identity;
        }
        var newIdentity = augmentIdentity(identity);
        if (newIdentity == null) {
            return identity;
        }

        return newIdentity;
    }

    protected SecurityIdentity augmentIdentity(SecurityIdentity securityIdentity) {
        Set<String> roles = securityIdentity.getRoles();
        if (roles != null && !roles.isEmpty()) {
            Set<Permission> permissions = grantPermissions ? new HashSet<>() : null;
            Set<String> newRoles = grantRoles ? new HashSet<>() : null;
            for (String role : roles) {
                if (grantPermissions) {
                    if (roleToPermissions.containsKey(role)) {
                        permissions.addAll(roleToPermissions.get(role));
                    }
                }
                if (grantRoles) {
                    if (roleToRoles.containsKey(role)) {
                        newRoles.addAll(roleToRoles.get(role));
                    }
                }
            }
            boolean addPerms = grantPermissions && !permissions.isEmpty();
            if (grantRoles && !newRoles.isEmpty()) {
                newRoles.addAll(roles);
                return augmentIdentity(securityIdentity, permissions, Set.copyOf(newRoles), addPerms);
            } else if (addPerms) {
                return augmentIdentity(securityIdentity, permissions, roles, true);
            }
        }
        return null;
    }

    private static SecurityIdentity augmentIdentity(SecurityIdentity securityIdentity, Set<Permission> permissions,
            Set<String> roles, boolean addPerms) {
        return new SecurityIdentity() {
            @Override
            public Principal getPrincipal() {
                return securityIdentity.getPrincipal();
            }

            @Override
            public boolean isAnonymous() {
                return securityIdentity.isAnonymous();
            }

            @Override
            public Set<String> getRoles() {
                return roles;
            }

            @Override
            public boolean hasRole(String s) {
                return roles.contains(s);
            }

            @Override
            public <T extends Credential> T getCredential(Class<T> aClass) {
                return securityIdentity.getCredential(aClass);
            }

            @Override
            public Set<Credential> getCredentials() {
                return securityIdentity.getCredentials();
            }

            @Override
            public <T> T getAttribute(String s) {
                return securityIdentity.getAttribute(s);
            }

            @Override
            public Map<String, Object> getAttributes() {
                return securityIdentity.getAttributes();
            }

            @Override
            public Uni<Boolean> checkPermission(Permission requiredPermission) {
                if (addPerms) {
                    for (Permission possessedPermission : permissions) {
                        if (possessedPermission.implies(requiredPermission)) {
                            return Uni.createFrom().item(true);
                        }
                    }
                }

                return securityIdentity.checkPermission(requiredPermission);
            }

            @Override
            public boolean checkPermissionBlocking(Permission requiredPermission) {
                if (addPerms) {
                    for (Permission possessedPermission : permissions) {
                        if (possessedPermission.implies(requiredPermission)) {
                            return true;
                        }
                    }
                }

                return securityIdentity.checkPermissionBlocking(requiredPermission);
            }
        };
    }
}
