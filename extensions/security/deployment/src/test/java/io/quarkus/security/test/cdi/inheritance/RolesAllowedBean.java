package io.quarkus.security.test.cdi.inheritance;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RolesAllowed("admin")
public class RolesAllowedBean {

    public String ping() {
        return RolesAllowedBean.class.getSimpleName();
    }
}
