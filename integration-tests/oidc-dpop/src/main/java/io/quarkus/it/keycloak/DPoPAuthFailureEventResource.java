package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/dpop-auth-failure")
public class DPoPAuthFailureEventResource {

    @Inject
    DPoPAuthFailureObserver observer;

    @GET
    @Path("/last-error")
    @Produces("text/plain")
    public String getLastError() {
        final String error = observer.getLastDPoPErrorAttribute();
        return error != null ? error : "";
    }

    @DELETE
    @Path("/last-error")
    public void resetLastError() {
        observer.reset();
    }
}
