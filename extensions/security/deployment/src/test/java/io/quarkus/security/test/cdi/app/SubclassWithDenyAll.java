package io.quarkus.security.test.cdi.app;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
@DenyAll
@ApplicationScoped
@Named(SubclassWithDenyAll.NAME)
public class SubclassWithDenyAll extends BeanWithSecuredMethods {

    public static final String NAME = "SubclassWithDenyAll";

    public String noAdditionalConstraints() {
        return "forbiddenOnClass";
    }

    @PermitAll
    public String allowedMethod() {
        return "allowedOnMethod";
    }

    @RolesAllowed("admin")
    public String restrictedOnMethod() {
        return "restrictedOnMethod";
    }
}
