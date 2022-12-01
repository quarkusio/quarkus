package io.quarkus.security.test.utils;

import java.util.Set;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class AuthData {
    public final Set<String> roles;
    public final boolean anonymous;
    public final String name;

    public AuthData(Set<String> roles, boolean anonymous, String name) {
        this.roles = roles;
        this.anonymous = anonymous;
        this.name = name;
    }
}
