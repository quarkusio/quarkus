package io.quarkus.security.test.utils;

import java.util.Set;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class AuthData {
    public final Set<String> roles;
    public final boolean anonymous;

    public AuthData(Set<String> roles, boolean anonymous) {
        this.roles = roles;
        this.anonymous = anonymous;
    }
}
