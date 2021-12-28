package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BeanImplementingInterfaceWithClassLevelAnnotation implements InterfaceWithClassLevelAnnotation {
    @Override
    public String allowedForAdmin() {
        return "accessibleForAdminOnly";
    }

}
