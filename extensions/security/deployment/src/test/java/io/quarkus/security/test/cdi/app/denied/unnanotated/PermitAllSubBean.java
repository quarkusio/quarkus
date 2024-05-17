package io.quarkus.security.test.cdi.app.denied.unnanotated;

import jakarta.annotation.security.DenyAll;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class PermitAllSubBean {
    public String unannotatedInSubclass() {
        return "unannotatedInSubclass";
    }

    @DenyAll
    public String deniedInSubclass() {
        return "deniedInSubclass";
    }
}
