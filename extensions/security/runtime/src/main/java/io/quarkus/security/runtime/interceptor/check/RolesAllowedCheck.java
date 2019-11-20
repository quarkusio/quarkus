package io.quarkus.security.runtime.interceptor.check;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;

public class RolesAllowedCheck implements SecurityCheck {

    /*
     * The reason we want to cache RolesAllowedCheck is that it is very common
     * to have a lot of methods using the same roles in the security check
     * In such cases there is no need to have multiple instances of the class hanging around
     * for the entire lifecycle of the application
     */
    private static final Map<List<String>, RolesAllowedCheck> CACHE = new ConcurrentHashMap<>();

    private final String[] allowedRoles;

    private RolesAllowedCheck(String[] allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public static RolesAllowedCheck of(String[] allowedRoles) {
        return CACHE.computeIfAbsent(Arrays.asList(allowedRoles), new Function<List<String>, RolesAllowedCheck>() {
            @Override
            public RolesAllowedCheck apply(List<String> allowedRolesList) {
                return new RolesAllowedCheck(allowedRolesList.toArray(new String[0]));
            }
        });
    }

    @Override
    public void apply(SecurityIdentity identity) {
        Set<String> roles = identity.getRoles();
        if (roles != null) {
            for (String role : allowedRoles) {
                if (roles.contains(role)) {
                    return;
                }
            }
        }
        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        } else {
            throw new ForbiddenException();
        }
    }
}
