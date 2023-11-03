package io.quarkus.security.test.cdi.app;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@PermitAll
@ApplicationScoped
public class SubclassWithPermitAll extends BeanWithSecuredMethods {
    public String allowedOnClass() {
        return "allowedOnClass";
    }

    @DenyAll
    public String forbiddenOnMethod() {
        return "forbiddenOnMethod";
    }

    @RolesAllowed("admin")
    public String restrictedOnMethod() {
        return "restrictedOnMethod";
    }
}
