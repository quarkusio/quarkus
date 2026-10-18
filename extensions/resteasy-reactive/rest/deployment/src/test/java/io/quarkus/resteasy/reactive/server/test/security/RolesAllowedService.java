package io.quarkus.resteasy.reactive.server.test.security;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;

@ApplicationScoped
public class RolesAllowedService {

    public static final String SERVICE_HELLO = "Hello from Service!";
    public static final String SERVICE_BYE = "Bye from Service!";
    public static final List<String> EVENT_BUS_MESSAGES = new CopyOnWriteArrayList<>();

    @RolesAllowed("admin")
    public String hello() {
        return SERVICE_HELLO;
    }

    @PermitAll
    public String bye() {
        return SERVICE_BYE;
    }

    @PermitAll
    @ActivateRequestContext
    void receivePermitAllMessage(String m) {
        EVENT_BUS_MESSAGES.add("permit all " + m);
    }

    @RolesAllowed("admin")
    @ActivateRequestContext
    void receiveRolesAllowedMessage(String m) {
        EVENT_BUS_MESSAGES.add("roles allowed " + m);
    }
}
