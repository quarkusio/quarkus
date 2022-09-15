package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.vertx.test.simple.PortProviderUtil;
import org.junit.jupiter.api.Assertions;

@Path("/UriInfoSimpleResource")
public class UriInfoSimpleResource {
    private static Logger LOG = Logger.getLogger(UriInfoSimpleResource.class);
    @Context
    UriInfo myInfo;

    public UriInfoSimpleResource() {
        System.out.println("Constructed");
    }

    @Path("/simple")
    @GET
    public String get(@Context UriInfo info, @QueryParam("abs") String abs) {
        LOG.debug("abs query: " + abs);
        URI base = null;
        if (abs == null) {
            base = PortProviderUtil.createURI("/");
        } else {
            base = PortProviderUtil.createURI("/" + abs + "/");
        }

        LOG.debug("BASE URI: " + info.getBaseUri());
        LOG.debug("Request URI: " + info.getRequestUri());
        LOG.debug("Absolute URI: " + info.getAbsolutePath());
        Assertions.assertEquals(base.getPath(), info.getBaseUri().getPath());
        Assertions.assertEquals("/UriInfoSimpleResource/simple", info.getPath());
        return "CONTENT";
    }

    @Path("/uri")
    @GET
    public String get(@Context UriInfo info) {
        return info.getRequestUri().toASCIIString();
    }

    @Path("/simple/fromField")
    @GET
    public String getField(@QueryParam("abs") String abs) {
        LOG.debug("abs query: " + abs);
        URI base = null;
        if (abs == null) {
            base = PortProviderUtil.createURI("/");
        } else {
            base = PortProviderUtil.createURI("/" + abs + "/");
        }

        LOG.debug("BASE URI: " + myInfo.getBaseUri());
        LOG.debug("Request URI: " + myInfo.getRequestUri());
        Assertions.assertEquals(base.getPath(), myInfo.getBaseUri().getPath());
        Assertions.assertEquals("/UriInfoSimpleResource/simple/fromField", myInfo.getPath());
        return "CONTENT";
    }

}
