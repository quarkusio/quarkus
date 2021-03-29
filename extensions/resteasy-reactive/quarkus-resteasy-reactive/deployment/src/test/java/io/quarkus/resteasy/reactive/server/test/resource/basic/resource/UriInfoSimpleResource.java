package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;

@Path("/UriInfoSimpleResource")
public class UriInfoSimpleResource {
    private static Logger LOG = Logger.getLogger(UriInfoSimpleResource.class);
    @Context
    UriInfo myInfo;

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
