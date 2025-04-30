package io.quarkus.resteasy.reactive.server.test.security;

import jakarta.enterprise.context.RequestScoped;

import io.quarkus.security.PermissionChecker;

@RequestScoped
public class BeanParamPermissionChecker {

    @PermissionChecker("say-hello")
    boolean canSayHello(String customAuthorizationHeader, String name, String query) {
        boolean queryParamAllowedForPermissionName = checkQueryParams(query);
        boolean usernameWhitelisted = isUserNameWhitelisted(name);
        boolean customAuthorizationMatches = checkCustomAuthorization(customAuthorizationHeader);
        return queryParamAllowedForPermissionName && usernameWhitelisted && customAuthorizationMatches;
    }

    static boolean checkCustomAuthorization(String customAuthorization) {
        return "customAuthorization".equals(customAuthorization);
    }

    static boolean isUserNameWhitelisted(String userName) {
        return "admin".equals(userName);
    }

    static boolean checkQueryParams(String queryParam) {
        return "myQueryParam".equals(queryParam);
    }
}
