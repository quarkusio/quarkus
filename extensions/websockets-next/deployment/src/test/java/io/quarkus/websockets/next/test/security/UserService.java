package io.quarkus.websockets.next.test.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

@RolesAllowed("user")
@ApplicationScoped
public class UserService {

    public String ping() {
        return "" + 42;
    }

}
