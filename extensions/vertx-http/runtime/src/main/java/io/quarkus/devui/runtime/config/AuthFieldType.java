package io.quarkus.devui.runtime.config;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum AuthFieldType {

    PERMISSION_ENABLED(AuthConfigType.PERMISSION, "enabled"),
    PERMISSION_POLICY(AuthConfigType.PERMISSION, "policy"),
    PERMISSION_METHODS(AuthConfigType.PERMISSION, "methods"),
    PERMISSION_PATHS(AuthConfigType.PERMISSION, "paths"),
    PERMISSION_AUTH_MECHANISM(AuthConfigType.PERMISSION, "auth-mechanism"),
    POLICY_ROLES_ALLOWED(AuthConfigType.POLICY, "roles-allowed"),
    POLICY_PERMISSIONS(AuthConfigType.POLICY, "permissions"),
    POLICY_PERMISSION_CLASS(AuthConfigType.POLICY, "permission-class");

    public enum AuthConfigType {
        PERMISSION("quarkus.http.auth.permission"),
        POLICY("quarkus.http.auth.policy");

        final String propertySuffix;

        AuthConfigType(String propertySuffix) {
            this.propertySuffix = propertySuffix;
        }
    }

    final AuthConfigType authConfigType;
    final String authProperty;

    AuthFieldType(AuthConfigType authConfigType, String authProperty) {
        this.authConfigType = authConfigType;
        this.authProperty = authProperty;
    }

    public AuthConfigType getAuthConfigType() {
        return authConfigType;
    }

    public Pattern getPattern() {
        String escapedPropertySuffix = this.authConfigType.propertySuffix.replaceAll("\\.", "\\\\.");
        String regexPattern = String.format("%s\\.(.+)\\.%s", escapedPropertySuffix, authProperty);
        return Pattern.compile(regexPattern);
    }

    public String convertToKey(String id) {
        return this.authConfigType.propertySuffix + "." + id + "." + this.authProperty;
    }

    public String convertToValue(String id, Object value) {
        if (this == AuthFieldType.PERMISSION_METHODS || this == AuthFieldType.PERMISSION_PATHS) {
            return (String) ((List) value).stream().collect(Collectors.joining(","));
        }

        return value instanceof Boolean ? ((Boolean) value).toString() : (String) value;
    }

}
