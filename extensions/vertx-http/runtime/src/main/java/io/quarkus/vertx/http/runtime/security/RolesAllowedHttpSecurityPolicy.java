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
    private List<String> rolesAllowed;
    private final boolean grantPermissions;
    private final Map<String, Set<Permission>> roleToPermissions;

    public RolesAllowedHttpSecurityPolicy(List<String> rolesAllowed) {
        this.rolesAllowed = rolesAllowed;
        this.grantPermissions = false;
        this.roleToPermissions = null;
    }

    public RolesAllowedHttpSecurityPolicy() {
        this.grantPermissions = false;
        this.roleToPermissions = null;
    }

    public RolesAllowedHttpSecurityPolicy(List<String> rolesAllowed, Map<String, Set<Permission>> roleToPermissions) {
        this.rolesAllowed = rolesAllowed;
        this.grantPermissions = true;
        this.roleToPermissions = roleToPermissions;
    }

    public List<String> getRolesAllowed() {
        return rolesAllowed;
    }

    public RolesAllowedHttpSecurityPolicy setRolesAllowed(List<String> rolesAllowed) {
        this.rolesAllowed = rolesAllowed;
        return this;
    }

    @Override
    public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
            AuthorizationRequestContext requestContext) {
        return identity.map(new Function<SecurityIdentity, CheckResult>() {
            @Override
            public CheckResult apply(SecurityIdentity securityIdentity) {
                for (String i : rolesAllowed) {
                    if (securityIdentity.hasRole(i) || ("**".equals(i) && !securityIdentity.isAnonymous())) {
                        if (grantPermissions) {
                            // permit access and add augment security identity with additional permissions
                            return grantPermissions(securityIdentity);
                        }
                        return CheckResult.PERMIT;
                    }
                }
                return CheckResult.DENY;
            }
        });
    }

    private CheckResult grantPermissions(SecurityIdentity securityIdentity) {
        Set<String> roles = securityIdentity.getRoles();
        if (roles != null && !roles.isEmpty()) {
            Set<Permission> permissions = new HashSet<>();
            for (String role : roles) {
                if (roleToPermissions.containsKey(role)) {
                    permissions.addAll(roleToPermissions.get(role));
                }
            }
            if (!permissions.isEmpty()) {
                return new CheckResult(true, augmentIdentity(securityIdentity, permissions));
            }
        }
        return CheckResult.PERMIT;
    }

    private static SecurityIdentity augmentIdentity(SecurityIdentity securityIdentity, Set<Permission> permissions) {
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
                return securityIdentity.getRoles();
            }

            @Override
            public boolean hasRole(String s) {
                return securityIdentity.hasRole(s);
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
                for (Permission possessedPermission : permissions) {
                    if (possessedPermission.implies(requiredPermission)) {
                        return Uni.createFrom().item(true);
                    }
                }

                return securityIdentity.checkPermission(requiredPermission);
            }

            @Override
            public boolean checkPermissionBlocking(Permission requiredPermission) {
                for (Permission possessedPermission : permissions) {
                    if (possessedPermission.implies(requiredPermission)) {
                        return true;
                    }
                }

                return securityIdentity.checkPermissionBlocking(requiredPermission);
            }
        };
    }
}
