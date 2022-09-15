package io.quarkus.security.deployment;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import org.jboss.jandex.DotName;

import io.quarkus.security.Authenticated;

public final class DotNames {

    public static final DotName ROLES_ALLOWED = DotName.createSimple(RolesAllowed.class.getName());
    public static final DotName AUTHENTICATED = DotName.createSimple(Authenticated.class.getName());
    public static final DotName DENY_ALL = DotName.createSimple(DenyAll.class.getName());
    public static final DotName PERMIT_ALL = DotName.createSimple(PermitAll.class.getName());

    private DotNames() {
    }
}
