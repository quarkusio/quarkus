package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BeanOverridingInterfaceWithMethodLevelAnnotations implements OverriddenInterfaceWithMethodLevelAnnotations {

    @PermitAll
    @Override
    public String overriddenMethod() {
        return "this should be permitted";
    }

    @DenyAll
    @Override
    public String otherOverriddenMethod() {
        return "this should be denied";
    }
}
