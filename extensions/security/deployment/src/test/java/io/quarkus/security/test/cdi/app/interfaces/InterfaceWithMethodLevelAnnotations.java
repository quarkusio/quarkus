package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;

public interface InterfaceWithMethodLevelAnnotations {

    @DenyAll
    String forbidden();

    @RolesAllowed("admin")
    String securedMethod();

}
