package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;

@Path("/frontend")
public class FrontendResource {
    @Inject
    @RestClient
    ProtectedResourceServiceCustomFilter protectedResourceServiceCustomFilter;

    @Inject
    @RestClient
    ProtectedResourceServiceReactiveFilter protectedResourceServiceReactiveFilter;

    @GET
    @Path("userNameCustomFilter")
    @Produces("text/plain")
    public Uni<String> userName() {
        return protectedResourceServiceCustomFilter.getUserName();
    }

    @GET
    @Path("userNameReactive")
    @Produces("text/plain")
    public Uni<String> userNameReactive() {
        return protectedResourceServiceReactiveFilter.getUserName();
    }
}
