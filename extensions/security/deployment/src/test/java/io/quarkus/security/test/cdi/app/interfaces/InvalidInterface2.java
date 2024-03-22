package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.annotation.security.RolesAllowed;

public interface InvalidInterface2 {
    @RolesAllowed("admin")
    String securedMethod();
}
