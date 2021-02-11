package io.quarkus.security.test.cdi.app;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;

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
