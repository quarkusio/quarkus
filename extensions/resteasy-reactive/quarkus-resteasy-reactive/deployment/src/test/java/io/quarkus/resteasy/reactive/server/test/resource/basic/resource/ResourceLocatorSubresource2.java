package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

public class ResourceLocatorSubresource2 {

    private static final Logger LOG = Logger.getLogger(ResourceLocatorSubresource2.class);

    @GET
    @Path("stuff/{param}/bar")
    public String doGet(@PathParam("param") String param, @Context UriInfo uri) {
        LOG.debug("Uri Ancestors for Subresource2.doGet():");
        Assertions.assertEquals(4, uri.getMatchedURIs().size());
        Assertions.assertEquals("base/1/resources/subresource2/stuff/2/bar", uri.getMatchedURIs().get(0));
        Assertions.assertEquals("base/1/resources/subresource2", uri.getMatchedURIs().get(1));
        Assertions.assertEquals("base/1/resources", uri.getMatchedURIs().get(2));
        Assertions.assertEquals("", uri.getMatchedURIs().get(3));
        for (String ancestor : uri.getMatchedURIs())
            LOG.debugv("   {0}", ancestor);

        LOG.debug("Uri Ancestors Object for Subresource2.doGet():");
        Assertions.assertEquals(3, uri.getMatchedResources().size());
        Assertions.assertEquals(ResourceLocatorSubresource2.class, uri.getMatchedResources().get(0).getClass());
        Assertions.assertEquals(ResourceLocatorSubresource.class, uri.getMatchedResources().get(1).getClass());
        Assertions.assertEquals(ResourceLocatorBaseResource.class, uri.getMatchedResources().get(2).getClass());
        for (Object ancestor : uri.getMatchedResources())
            LOG.debugv("   {0}", ancestor.getClass().getName());
        Assertions.assertEquals("2", param);
        return this.getClass().getName() + "-" + param;
    }
}
