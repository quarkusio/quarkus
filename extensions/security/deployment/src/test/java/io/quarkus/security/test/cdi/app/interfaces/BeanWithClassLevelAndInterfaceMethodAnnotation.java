package io.quarkus.security.test.cdi.app.interfaces;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RolesAllowed("admin")
public class BeanWithClassLevelAndInterfaceMethodAnnotation implements MixedAnnotationsInterface {

    @Override
    public String unannotatedMethod() {
        return "accessibleForAdminOnly";
    }

    @Override
    public String denyAllMethod() {
        return "shouldBeDenied";
    }

}
