package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;

@Path("/")
public class ResponseInfoResource {
    private static Logger LOG = Logger.getLogger(ResponseInfoResource.class);

    @Path("/simple")
    @GET
    public String get(@QueryParam("abs") String abs) {
        LOG.debug("abs query: " + abs);
        URI base;
        if (abs == null) {
            base = PortProviderUtil.createURI("/new/one");
        } else {
            base = PortProviderUtil.createURI("/" + abs + "/new/one");
        }
        Response response = Response.temporaryRedirect(URI.create("new/one")).build();
        URI uri = (URI) response.getMetadata().getFirst(HttpHeaders.LOCATION);
        LOG.debug("Location uri: " + uri);
        Assertions.assertEquals(base.getPath(), uri.getPath());
        return "CONTENT";
    }
}
