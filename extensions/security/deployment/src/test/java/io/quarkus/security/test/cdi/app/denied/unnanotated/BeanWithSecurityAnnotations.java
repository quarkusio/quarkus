package io.quarkus.security.test.cdi.app.denied.unnanotated;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import io.quarkus.security.Authenticated;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@ApplicationScoped
@Named(BeanWithSecurityAnnotations.NAME)
public class BeanWithSecurityAnnotations {
    public static final String NAME = "BeanWithSecurityAnnotations";

    public String unannotated() {
        return "unannotated";
    }

    @PermitAll
    public String allowed() {
        return "allowed";
    }

    @RolesAllowed("admin")
    public String restricted() {
        return "accessibleForAdminOnly";
    }

    @Authenticated
    public String authenticated() {
        return "authenticated";
    }

    @DenyAll
    public String deny() {
        return "ignored";
    }
}
