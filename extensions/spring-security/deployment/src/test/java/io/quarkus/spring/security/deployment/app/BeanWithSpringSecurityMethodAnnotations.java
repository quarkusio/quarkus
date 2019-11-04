package io.quarkus.spring.security.deployment.app;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.springframework.security.access.annotation.Secured;

@ApplicationScoped
@Named(BeanWithSpringSecurityMethodAnnotations.NAME)
public class BeanWithSpringSecurityMethodAnnotations {
    public static final String NAME = "BeanWithSpringSecurityMethodAnnotations";

    public String unannotated() {
        return "unannotated";
    }

    @Secured("admin")
    public String restricted() {
        return "accessibleForAdminOnly";
    }
}
