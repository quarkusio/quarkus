package io.quarkus.security.deployment;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import org.jboss.jandex.DotName;

import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;

public final class DotNames {

    public static final DotName ROLES_ALLOWED = DotName.createSimple(RolesAllowed.class.getName());
    public static final DotName AUTHENTICATED = DotName.createSimple(Authenticated.class.getName());
    public static final DotName PERMISSIONS_ALLOWED = DotName.createSimple(PermissionsAllowed.class.getName());
    public static final DotName DENY_ALL = DotName.createSimple(DenyAll.class.getName());
    public static final DotName PERMIT_ALL = DotName.createSimple(PermitAll.class.getName());

    private DotNames() {
    }
}
