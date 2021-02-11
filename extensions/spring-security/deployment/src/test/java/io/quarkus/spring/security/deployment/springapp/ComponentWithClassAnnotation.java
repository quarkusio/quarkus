package io.quarkus.spring.security.deployment.springapp;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
@PreAuthorize("hasRole('user')")
public class ComponentWithClassAnnotation {

    public String unannotated() {
        return "unannotated";
    }

    @PreAuthorize("hasRole('admin')")
    public String restrictedOnMethod() {
        return "restrictedOnMethod";
    }

    @Secured("user")
    public String securedWithSecuredAnnotation() {
        return "securedWithSecuredAnnotation";
    }
}
