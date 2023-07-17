package io.quarkus.security.test.cdi;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Singleton;

@Singleton
public class BeanWithSecuredFinalMethod {

    @RolesAllowed("admin")
    public final String securedMethod() {
        return "accessibleForAdminOnly";
    }

    @DenyAll
    public final String otherSecuredMethod(String input) {
        return "denied";
    }
}
