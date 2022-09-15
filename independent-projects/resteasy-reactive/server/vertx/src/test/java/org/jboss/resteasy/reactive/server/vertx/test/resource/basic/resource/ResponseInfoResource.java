package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.vertx.test.simple.PortProviderUtil;
import org.junit.jupiter.api.Assertions;

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
