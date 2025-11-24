package io.quarkus.hibernate.reactive.rest.data.panache.deployment.security;

import jakarta.inject.Singleton;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

@Singleton
public final class PermissionCheckerBean {

    @PermissionChecker("list-empty")
    boolean canListEmpty(SecurityIdentity identity) {
        return identity.hasRole("admin");
    }

    @PermissionChecker("find-by-name-1")
    boolean canFindByName1(SecurityIdentity identity) {
        return identity.hasRole("admin") || identity.hasRole("user");
    }

    @PermissionChecker("find-by-name-2")
    boolean canFindByName2(SecurityIdentity identity) {
        return identity.hasRole("admin");
    }

    @PermissionChecker("get-1")
    boolean canGet1(SecurityIdentity identity) {
        return identity.hasRole("admin") || identity.hasRole("user");
    }

    @PermissionChecker("get-2")
    boolean canGet2(SecurityIdentity identity) {
        return identity.hasRole("admin");
    }

    @PermissionChecker("add")
    boolean canAdd(SecurityIdentity identity) {
        return identity.hasRole("admin");
    }
}
