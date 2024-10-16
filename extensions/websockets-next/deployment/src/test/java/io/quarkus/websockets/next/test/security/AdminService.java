package io.quarkus.websockets.next.test.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

@RolesAllowed("admin")
@ApplicationScoped
public class AdminService {

    public String ping() {
        return "" + 24;
    }

}
