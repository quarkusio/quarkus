package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

import io.quarkus.vertx.http.HttpServer;

@Path("UriInfoSimpleSingletonResource")
public class UriInfoSimpleSingletonResource {
    private static final Logger LOG = Logger.getLogger(UriInfoSimpleSingletonResource.class);

    @Inject
    HttpServer server;
    @Context
    UriInfo myInfo;

    @Path("/simple")
    @GET
    public String get(@Context UriInfo info, @QueryParam("abs") String abs) {
        LOG.debug("abs query: " + abs);
        URI base;
        if (abs == null) {
            base = UriBuilder.fromUri(server.getLocalBaseUri()).path("/").build();
        } else {
            base = UriBuilder.fromUri(server.getLocalBaseUri()).path("/").path(abs).build();
        }

        LOG.debug("BASE URI: " + info.getBaseUri());
        LOG.debug("Request URI: " + info.getRequestUri());
        Assertions.assertEquals(base.getPath(), info.getBaseUri().getPath());
        Assertions.assertEquals("/simple", info.getPath());
        return "CONTENT";
    }

    @Path("/simple/fromField")
    @GET
    public String get(@QueryParam("abs") String abs) {
        LOG.debug("abs query: " + abs);
        URI base;
        if (abs == null) {
            base = UriBuilder.fromUri(server.getLocalBaseUri()).path("/").build();
        } else {
            base = UriBuilder.fromUri(server.getLocalBaseUri()).path("/").path(abs).build();
        }

        LOG.debug("BASE URI: " + myInfo.getBaseUri());
        LOG.debug("Request URI: " + myInfo.getRequestUri());
        Assertions.assertEquals(base.getPath(), myInfo.getBaseUri().getPath());
        Assertions.assertEquals("/simple/fromField", myInfo.getPath());
        return "CONTENT";
    }

}
