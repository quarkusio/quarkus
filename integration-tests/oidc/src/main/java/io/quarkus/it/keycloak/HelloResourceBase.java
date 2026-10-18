package io.quarkus.it.keycloak;

import jakarta.annotation.security.RolesAllowed;

@RolesAllowed("user")
public class HelloResourceBase {

    public String hello() {
        return "hello";
    }
}
