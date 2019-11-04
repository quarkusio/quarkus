package io.quarkus.it.spring.security.security;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

@Service("serviceWithSecuredMethods")
public class ServiceWithSecuredMethods {

    @Secured("admin")
    public String securedMethod() {
        return "accessibleForAdminOnly";
    }

    public String accessibleForAll() {
        return "accessibleForAll";
    }
}
