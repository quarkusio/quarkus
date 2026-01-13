package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

import io.quarkus.vertx.http.HttpServer;

@Path("/")
public class ResponseInfoResource {
    private static final Logger LOG = Logger.getLogger(ResponseInfoResource.class);

    @Inject
    HttpServer server;

    @Path("/simple")
    @GET
    public String get(@QueryParam("abs") String abs) {
        LOG.debug("abs query: " + abs);
        URI base;
        if (abs == null) {
            base = UriBuilder.fromUri(server.getLocalBaseUri()).path("/new/one").build();
        } else {
            base = UriBuilder.fromUri(server.getLocalBaseUri()).path("/").path(abs).path("/new/one").build();
        }
        Response response = Response.temporaryRedirect(URI.create("new/one")).build();
        URI uri = (URI) response.getMetadata().getFirst(HttpHeaders.LOCATION);
        LOG.debug("Location uri: " + uri);
        Assertions.assertEquals(base.getPath(), uri.getPath());
        return "CONTENT";
    }
}
