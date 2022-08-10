package io.quarkus.resteasy.reactive.server.test.security;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RolesAllowedService {

    public static final String SERVICE_HELLO = "Hello from Service!";

    @RolesAllowed("admin")
    public String hello() {
        return SERVICE_HELLO;
    }

}
