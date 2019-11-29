package io.quarkus.spring.security.deployment.springapp;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;

@Component
public class BeanWithMetaAnnotations {

    @IsUser
    public String preAuthorizeMetaAnnotationIsUser() {
        return "preAuthorizeMetaAnnotationIsUser";
    }

    @IsUserOrAdmin
    public String preAuthorizeMetaAnnotationIsUserOrAdmin() {
        return "preAuthorizeMetaAnnotationIsUserOrAdmin";
    }

    public String notSecured() {
        return "notSecured";
    }

    @Secured("user")
    public String isSecuredWithSecuredAnnotation() {
        return "isSecuredWithSecuredAnnotation";
    }
}
