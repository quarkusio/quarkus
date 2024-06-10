package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.annotation.security.DenyAll;

public interface InvalidInterface1 {
    @DenyAll
    String securedMethod();
}
