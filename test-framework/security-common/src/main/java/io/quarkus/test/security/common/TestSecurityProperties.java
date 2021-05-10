package io.quarkus.test.security.common;

import java.util.Collections;
import java.util.Map;

public class TestSecurityProperties {
    /**
     * If this is non-zero then the test will be run with a SecurityIdentity with the specified username.
     */
    private final String user;

    private final String[] roles;

    private final SecurityAttribute[] attributes;

    private final Map<String, String> extraProperties;

    /**
     * If this is false then all security constraints are disabled.
     */
    private final boolean authorizationEnabled;

    public TestSecurityProperties(String user, String[] roles,
            SecurityAttribute[] attributes, boolean authorizationEnabled) {
        this(user, roles, attributes, authorizationEnabled, Collections.emptyMap());
    }

    public TestSecurityProperties(String user, String[] roles,
            SecurityAttribute[] attributes, boolean authorizationEnabled, Map<String, String> extraProperties) {
        this.user = user;
        this.roles = roles;
        this.attributes = attributes;
        this.authorizationEnabled = authorizationEnabled;
        this.extraProperties = extraProperties;
    }

    public String getUser() {
        return user;
    }

    public String[] getRoles() {
        return roles;
    }

    public SecurityAttribute[] getAttributes() {
        return attributes;
    }

    public boolean isAuthorizationEnabled() {
        return authorizationEnabled;
    }

    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }
}
