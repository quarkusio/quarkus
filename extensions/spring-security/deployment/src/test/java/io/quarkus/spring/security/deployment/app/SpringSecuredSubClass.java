package io.quarkus.spring.security.deployment.app;

import jakarta.enterprise.context.ApplicationScoped;

import org.springframework.security.access.annotation.Secured;

@ApplicationScoped
public class SpringSecuredSubClass extends BeanWithSpringSecurityMethodAnnotations {

    @Override
    @Secured("user")
    public String restricted() {
        return "restrictedOnMethod";
    }
}
