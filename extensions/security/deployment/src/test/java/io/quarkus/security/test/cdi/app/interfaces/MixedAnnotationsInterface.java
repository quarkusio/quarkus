package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.annotation.security.DenyAll;

public interface MixedAnnotationsInterface {

    String unannotatedMethod();

    @DenyAll
    String denyAllMethod();
}
