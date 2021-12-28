package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.annotation.security.DenyAll;

@DenyAll
public interface OverriddenInterfaceWithClassLevelAnnotation {

    String overriddenMethod();

}
