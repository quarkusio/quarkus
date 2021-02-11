package io.quarkus.security.test.cdi;

import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;

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
