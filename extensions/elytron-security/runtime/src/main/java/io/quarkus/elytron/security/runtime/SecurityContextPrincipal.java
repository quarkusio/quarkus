package io.quarkus.elytron.security.runtime;

import java.security.Principal;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;

@RequestScoped
public class SecurityContextPrincipal {
    Principal principal;

    @Produces
    @RequestScoped
    Principal getContextPrincipal() {
        return principal;
    }

    void setContextPrincipal(Principal principal) {
        this.principal = principal;
    }
}
