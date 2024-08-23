package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("authentication-event")
public class AuthenticationEventResource {

    @Inject
    AuthenticationFailureEventListener listener;

    @Authenticated
    @GET
    @Path("secured")
    public String secured() {
        return "ignored";
    }

    @GET
    @Path("failure-observed")
    public boolean authFailureObserved() {
        return listener.getFailedPaths().stream().anyMatch(p -> p.contains("authentication-event/secured"));
    }

}
