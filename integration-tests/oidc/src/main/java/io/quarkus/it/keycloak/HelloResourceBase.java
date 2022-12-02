package io.quarkus.it.keycloak;

import javax.annotation.security.RolesAllowed;

@RolesAllowed("user")
public class HelloResourceBase {

    public String hello() {
        return "hello";
    }
}
