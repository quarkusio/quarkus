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
import io.vertx.ext.web.RoutingContext;

/**
 * permission checker that handles role based permissions
 */
public class RolesAllowedHttpSecurityPolicy implements HttpSecurityPolicy {
    private static final String AUTHENTICATED = "**";
    private final String[] rolesAllowed;
    private final boolean grantPermissions;
    private final boolean grantRoles;
    private final Map<String, Set<Permission>> roleToPermissions;
    private final Map<String, List<String>> roleToRoles;

    public RolesAllowedHttpSecurityPolicy(List<String> rolesAllowed, Map<String, Set<Permission>> roleToPermissions,
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
        this.rolesAllowed = rolesAllowed.toArray(String[]::new);
    }

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return identity.map(new Function<SecurityIdentity, CheckResult>() {
            @Override
            public CheckResult apply(SecurityIdentity securityIdentity) {
                if (grantPermissions || grantRoles) {
                    SecurityIdentity augmented = augmentIdentity(securityIdentity);
                    if (augmented != null) {
                        for (String i : rolesAllowed) {
                            if (augmented.hasRole(i) || (AUTHENTICATED.equals(i) && !augmented.isAnonymous())) {
                                return new CheckResult(true, augmented);
                            }
                        }
                        return CheckResult.DENY;
                    }
                }
                for (String i : rolesAllowed) {
                    if (securityIdentity.hasRole(i) || (AUTHENTICATED.equals(i) && !securityIdentity.isAnonymous())) {
                        return CheckResult.PERMIT;
                    }
                }
                return CheckResult.DENY;
            }
        });
    }

    private SecurityIdentity augmentIdentity(SecurityIdentity securityIdentity) {
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
