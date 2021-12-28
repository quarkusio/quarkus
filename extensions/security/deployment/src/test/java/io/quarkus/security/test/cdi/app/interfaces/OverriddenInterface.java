package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;

public interface OverriddenInterface {

    @DenyAll
    String overriddenMethod();

    @PermitAll
    String otherOverriddenMethod();
}
