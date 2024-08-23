package io.quarkus.it.rest;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.Authenticated;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@ApplicationScoped
public class RBACBean {

    @Authenticated
    public String authenticated() {
        return "callingAuthenticated";
    }

    @RolesAllowed("**")
    public String allRoles() {
        return "callingAllRoles";
    }

    @PermitAll
    public String permitted() {
        return "callingPermitted";
    }

    @RolesAllowed("tester")
    public String testerOnly() {
        return "callingTesterOnly";
    }

    @DenyAll
    public String denied() {
        return "callingDenied";
    }
}
