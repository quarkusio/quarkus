package io.quarkus.it.spring.security.security;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

@Service("securedService")
@Secured("user")
public class SecuredService {

    public String noAdditionalConstraints() {
        return "restrictedOnClass";
    }

    @Secured("admin")
    public String restrictedOnMethod() {
        return "restrictedOnMethod";
    }
}
