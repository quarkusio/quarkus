package io.quarkus.it.rest;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;

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
