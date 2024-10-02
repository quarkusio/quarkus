package io.quarkus.resteasy.reactive.server.test.security;

import java.security.Permission;

public class OtherBeanParamPermission extends Permission {

    private final String actions;

    public OtherBeanParamPermission(String permissionName, String customAuthorizationHeader, String name, String query) {
        super(permissionName);
        this.actions = computeActions(customAuthorizationHeader, name, query);
    }

    @Override
    public String getActions() {
        return actions;
    }

    @Override
    public boolean implies(Permission p) {
        boolean nameMatches = getName().equals(p.getName());
        boolean actionMatches = getActions().equals(p.getActions());
        return nameMatches && actionMatches;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    private static String computeActions(String customAuthorizationHeader, String name, String query) {
        boolean queryParamAllowedForPermissionName = checkQueryParams(query);
        boolean usernameWhitelisted = isUserNameWhitelisted(name);
        boolean customAuthorizationMatches = checkCustomAuthorization(customAuthorizationHeader);
        var isAuthorized = queryParamAllowedForPermissionName && usernameWhitelisted && customAuthorizationMatches;
        if (isAuthorized) {
            return "hello";
        } else {
            return "goodbye";
        }
    }

    private static boolean checkCustomAuthorization(String customAuthorization) {
        return "customAuthorization".equals(customAuthorization);
    }

    private static boolean isUserNameWhitelisted(String userName) {
        return "admin".equals(userName);
    }

    private static boolean checkQueryParams(String queryParam) {
        return "myQueryParam".equals(queryParam);
    }

}
