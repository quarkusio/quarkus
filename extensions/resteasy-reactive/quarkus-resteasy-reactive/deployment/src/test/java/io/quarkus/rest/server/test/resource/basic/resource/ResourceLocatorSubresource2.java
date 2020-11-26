package io.quarkus.rest.server.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

public class ResourceLocatorSubresource2 {

    private static final Logger LOG = Logger.getLogger(ResourceLocatorSubresource2.class);

    @GET
    @Path("stuff/{param}/bar")
    public String doGet(@PathParam("param") String param, @Context UriInfo uri) {
        LOG.info("Uri Ancesstors for Subresource2.doGet():");
        Assertions.assertEquals(4, uri.getMatchedURIs().size());
        Assertions.assertEquals("base/1/resources/subresource2/stuff/2/bar", uri.getMatchedURIs().get(0));
        Assertions.assertEquals("base/1/resources/subresource2", uri.getMatchedURIs().get(1));
        Assertions.assertEquals("base/1/resources", uri.getMatchedURIs().get(2));
        Assertions.assertEquals("", uri.getMatchedURIs().get(3));
        for (String ancestor : uri.getMatchedURIs())
            LOG.infov("   {0}", ancestor);

        LOG.info("Uri Ancesstors Object for Subresource2.doGet():");
        Assertions.assertEquals(3, uri.getMatchedResources().size());
        Assertions.assertEquals(ResourceLocatorSubresource2.class, uri.getMatchedResources().get(0).getClass());
        Assertions.assertEquals(ResourceLocatorSubresource.class, uri.getMatchedResources().get(1).getClass());
        Assertions.assertEquals(ResourceLocatorBaseResource.class, uri.getMatchedResources().get(2).getClass());
        for (Object ancestor : uri.getMatchedResources())
            LOG.infov("   {0}", ancestor.getClass().getName());
        Assertions.assertEquals("2", param);
        return this.getClass().getName() + "-" + param;
    }
}
