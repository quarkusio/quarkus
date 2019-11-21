package io.quarkus.spring.security.deployment.springapp;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
@PreAuthorize("denyAll()")
public class DenyAllOnClass {

    public String noAnnotation() {
        return "noAnnotation";
    }

    @PreAuthorize("permitAll()")
    public String permitAll() {
        return "permitAll";
    }

    @Secured("admin")
    public String annotatedWithSecured() {
        return "annotatedWithSecured";
    }
}
