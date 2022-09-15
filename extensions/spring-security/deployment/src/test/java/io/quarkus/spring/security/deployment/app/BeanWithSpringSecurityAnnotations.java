package io.quarkus.spring.security.deployment.app;

import jakarta.enterprise.context.ApplicationScoped;

import org.springframework.security.access.annotation.Secured;

@ApplicationScoped
@Secured("admin")
public class BeanWithSpringSecurityAnnotations extends BeanWithSpringSecurityMethodAnnotations {

    public String restricted() {
        return "accessibleForAdminOnly";
    }

    @Secured("user")
    public String restrictedOnMethod() {
        return "accessibleForUserOnly";
    }
}
