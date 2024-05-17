package io.quarkus.security.test.cdi.app.denied.unnanotated;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;


@PermitAll
@ApplicationScoped
public class PermitAllBean {

    public String unannotated() {
        return "unannotated";
    }
}
