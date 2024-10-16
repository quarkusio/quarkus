package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;

@Path("UriInfoSimpleSingletonResource")
public class UriInfoSimpleSingletonResource {
    private static Logger LOG = Logger.getLogger(UriInfoSimpleSingletonResource.class);

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
        Assertions.assertEquals(base.getPath(), info.getBaseUri().getPath());
        Assertions.assertEquals("/simple", info.getPath());
        return "CONTENT";
    }

    @Path("/simple/fromField")
    @GET
    public String get(@QueryParam("abs") String abs) {
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
        Assertions.assertEquals("/simple/fromField", myInfo.getPath());
        return "CONTENT";
    }

}
