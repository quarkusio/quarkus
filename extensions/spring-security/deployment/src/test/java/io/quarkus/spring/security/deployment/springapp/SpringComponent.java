package io.quarkus.spring.security.deployment.springapp;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class SpringComponent {

    @PreAuthorize("hasRole(@roles.ADMIN)")
    public String accessibleForAdminOnly() {
        return "accessibleForAdminOnly";
    }

    @PreAuthorize("hasRole(@roles.USER)")
    public String accessibleForUserOnly() {
        return "accessibleForUserOnly";
    }

    @PreAuthorize("hasRole('user')")
    public String accessibleForUserOnlyString() {
        return "accessibleForUserOnlyString";
    }

    @PreAuthorize("#username == authentication.principal.username")
    public String principalNameIs(Object something, String username, Object somethingElse) {
        return username;
    }

    @PreAuthorize("#name != authentication.principal.username")
    public String principalNameIsNot(String name) {
        return name;
    }

    @PreAuthorize("#person.name == authentication.principal.username")
    public String principalNameFromObject(Person person) {
        return person.getName();
    }

    public String notSecured() {
        return "notSecured";
    }

    @Secured("admin")
    public String securedWithSecuredAnnotation() {
        return "securedWithSecuredAnnotation";
    }
}
