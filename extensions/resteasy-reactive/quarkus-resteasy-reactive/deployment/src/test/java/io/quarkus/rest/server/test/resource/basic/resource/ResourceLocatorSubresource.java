package io.quarkus.rest.server.test.resource.basic.resource;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

public class ResourceLocatorSubresource {

    private static final Logger LOG = Logger.getLogger(ResourceLocatorSubresource.class);

    @GET
    public String doGet(@Context UriInfo uri) {
        LOG.info("Uri Ancesstors for Subresource.doGet():");
        List<String> matchedURIs = uri.getMatchedURIs();
        Assertions.assertEquals(2, matchedURIs.size());
        Assertions.assertEquals("base/1/resources", matchedURIs.get(0));
        Assertions.assertEquals("", matchedURIs.get(1));
        for (String ancestor : matchedURIs)
            LOG.info("   " + ancestor);

        LOG.info("Uri Ancesstors Object for Subresource.doGet():");
        Assertions.assertEquals(2, uri.getMatchedResources().size());
        Assertions.assertEquals(ResourceLocatorSubresource.class, uri.getMatchedResources().get(0).getClass());
        Assertions.assertEquals(ResourceLocatorBaseResource.class, uri.getMatchedResources().get(1).getClass());
        for (Object ancestor : uri.getMatchedResources())
            LOG.infov("   {0}", ancestor.getClass().getName());
        return this.getClass().getName();
    }

    @Path("/subresource2")
    public Object getSubresource2(@Context UriInfo uri) {
        LOG.info("Uri Ancesstors for Subresource.getSubresource2():");
        List<String> matchedURIs = uri.getMatchedURIs();
        Assertions.assertEquals(3, matchedURIs.size());
        Assertions.assertEquals("base/1/resources/subresource2", matchedURIs.get(0));
        Assertions.assertEquals("base/1/resources", matchedURIs.get(1));
        Assertions.assertEquals("", matchedURIs.get(2));
        for (String ancestor : matchedURIs)
            LOG.info("   " + ancestor);

        LOG.info("Uri Ancesstors Object for Subresource.getSubresource2():");
        Assertions.assertEquals(2, uri.getMatchedResources().size());
        Assertions.assertEquals(ResourceLocatorSubresource.class, uri.getMatchedResources().get(0).getClass());
        Assertions.assertEquals(ResourceLocatorBaseResource.class, uri.getMatchedResources().get(1).getClass());
        for (Object ancestor : uri.getMatchedResources())
            LOG.infov("   {0}", ancestor.getClass().getName());
        return new ResourceLocatorSubresource2();
    }
}
