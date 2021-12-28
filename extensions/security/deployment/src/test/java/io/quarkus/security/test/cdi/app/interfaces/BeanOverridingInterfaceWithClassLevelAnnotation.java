package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;

@PermitAll
@ApplicationScoped
public class BeanOverridingInterfaceWithClassLevelAnnotation implements OverriddenInterfaceWithClassLevelAnnotation {

    @Override
    public String overriddenMethod() {
        return "this should be permitted";
    }

}
