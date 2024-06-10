package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.annotation.security.RolesAllowed;

@RolesAllowed("admin")
public interface InterfaceWithClassLevelAnnotation {

    String allowedForAdmin();

}
