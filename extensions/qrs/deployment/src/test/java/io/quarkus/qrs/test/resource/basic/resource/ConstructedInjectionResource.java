package io.quarkus.qrs.test.resource.basic.resource;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

import io.quarkus.qrs.test.PortProviderUtil;

@Path("/")
public class ConstructedInjectionResource {
    private static Logger logger = Logger.getLogger(ConstructedInjectionResource.class);

    UriInfo myInfo;
    String abs;

    public ConstructedInjectionResource(@Context final UriInfo myInfo, @QueryParam("abs") final String abs) {
        this.myInfo = myInfo;
        this.abs = abs;
    }

    @Path("/simple")
    @GET
    public String get() {
        logger.info("abs query: " + abs);
        URI base = null;
        if (abs == null) {
            base = PortProviderUtil.createURI("/");
        } else {
            base = PortProviderUtil.createURI("/" + abs + "/");
        }

        logger.info("BASE URI: " + myInfo.getBaseUri());
        logger.info("Request URI: " + myInfo.getRequestUri());
        Assertions.assertEquals("The injected base path doesn't match to the expected one",
                base.getPath() + "ConstructedInjectionTest/", myInfo.getBaseUri().getPath());
        Assertions.assertEquals("The injected path doesn't match to the expected one", "/simple", myInfo.getPath());
        return "CONTENT";
    }

}
