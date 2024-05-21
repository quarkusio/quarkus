package io.quarkus.resteasy.test.security;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RolesAllowedService {

    public static final String SERVICE_HELLO = "Hello from Service!";
    public static final String SERVICE_BYE = "Bye from Service!";

    @RolesAllowed("admin")
    public String hello() {
        return SERVICE_HELLO;
    }

    @PermitAll
    public String bye() {
        return SERVICE_BYE;
    }

}
