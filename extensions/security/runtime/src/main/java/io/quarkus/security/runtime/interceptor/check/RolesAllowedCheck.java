package io.quarkus.security.runtime.interceptor.check;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.SecurityCheck;

public class RolesAllowedCheck implements SecurityCheck {

    /*
     * The reason we want to cache RolesAllowedCheck is that it is very common
     * to have a lot of methods using the same roles in the security check
     * In such cases there is no need to have multiple instances of the class hanging around
     * for the entire lifecycle of the application
     */
    private static final Map<Collection<String>, RolesAllowedCheck> CACHE = new ConcurrentHashMap<>();

    private final String[] allowedRoles;

    private RolesAllowedCheck(String[] allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public static RolesAllowedCheck of(String[] allowedRoles) {
        return CACHE.computeIfAbsent(getCollectionForKey(allowedRoles), new Function<Collection<String>, RolesAllowedCheck>() {
            @Override
            public RolesAllowedCheck apply(Collection<String> allowedRolesList) {
                return new RolesAllowedCheck(allowedRolesList.toArray(new String[0]));
            }
        });
    }

    private static Collection<String> getCollectionForKey(String[] allowedRoles) {
        if (allowedRoles.length == 0) { // shouldn't happen, but lets be on the safe side
            return Collections.emptyList();
        } else if (allowedRoles.length == 1) {
            return Collections.singletonList(allowedRoles[0]);
        }
        // use a set in order to avoid caring about the order of elements
        return new HashSet<>(Arrays.asList(allowedRoles));
    }

    @Override
    public void apply(SecurityIdentity identity, Method method, Object[] parameters) {
        for (String role : allowedRoles) {
            if (identity.hasRole(role) || ("**".equals(role) && !identity.isAnonymous())) {
                return;
            }
        }
        if (identity.isAnonymous()) {
            throw new UnauthorizedException();
        } else {
            throw new ForbiddenException();
        }
    }
}
