package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BeanImplementingInterfaceWithMethodLevelAnnotations
        implements InterfaceWithMethodLevelAnnotations {
    @Override
    public String forbidden() {
        return "shouldBeDenied";
    }

    @Override
    public String securedMethod() {
        return "accessibleForAdminOnly";
    }
}
