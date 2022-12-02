package io.quarkus.it.keycloak;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
