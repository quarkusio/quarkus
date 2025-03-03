package io.quarkus.security.test.utils;

import java.security.Permission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class AuthData {
    public final Set<String> roles;
    public final boolean anonymous;
    public final String name;
    public final Set<Permission> permissions;
    public final boolean applyAugmentors;

    public AuthData(Set<String> roles, boolean anonymous, String name) {
        this.roles = roles;
        this.anonymous = anonymous;
        this.name = name;
        this.permissions = null;
        this.applyAugmentors = false;
    }

    public AuthData(Set<String> roles, boolean anonymous, String name, Set<Permission> permissions) {
        this.roles = roles;
        this.anonymous = anonymous;
        this.name = name;
        this.permissions = permissions;
        this.applyAugmentors = false;
    }

    public AuthData(Set<String> roles, boolean anonymous, String name, Set<Permission> permissions, boolean applyAugmentors) {
        this.roles = roles;
        this.anonymous = anonymous;
        this.name = name;
        this.permissions = permissions;
        this.applyAugmentors = applyAugmentors;
    }

    public AuthData(AuthData authData, boolean applyAugmentors) {
        this(authData.roles, authData.anonymous, authData.name, authData.permissions, applyAugmentors);
    }

    public AuthData(AuthData authData, boolean applyAugmentors, Permission... permissions) {
        this(authData.roles, authData.anonymous, authData.name, new HashSet<>(Arrays.asList(permissions)), applyAugmentors);
    }
}
