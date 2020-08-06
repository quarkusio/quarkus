package io.quarkus.qrs.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.junit.Assert;

public class ResourceLocatorSubresource2{

   private static final Logger LOG = Logger.getLogger(ResourceLocatorSubresource2.class);

   @GET
   @Path("stuff/{param}/bar")
   public String doGet(@PathParam("param") String param, @Context UriInfo uri) {
      LOG.info("Uri Ancesstors for Subresource2.doGet():");
      Assert.assertEquals(4, uri.getMatchedURIs().size());
      Assert.assertEquals("base/1/resources/subresource2/stuff/2/bar", uri.getMatchedURIs().get(0));
      Assert.assertEquals("base/1/resources/subresource2", uri.getMatchedURIs().get(1));
      Assert.assertEquals("base/1/resources", uri.getMatchedURIs().get(2));
      Assert.assertEquals("", uri.getMatchedURIs().get(3));
      for (String ancestor : uri.getMatchedURIs()) LOG.infov("   {0}", ancestor);


      LOG.info("Uri Ancesstors Object for Subresource2.doGet():");
      Assert.assertEquals(3, uri.getMatchedResources().size());
      Assert.assertEquals(ResourceLocatorSubresource2.class, uri.getMatchedResources().get(0).getClass());
      Assert.assertEquals(ResourceLocatorSubresource.class, uri.getMatchedResources().get(1).getClass());
      Assert.assertEquals(ResourceLocatorBaseResource.class, uri.getMatchedResources().get(2).getClass());
      for (Object ancestor : uri.getMatchedResources()) LOG.infov("   {0}", ancestor.getClass().getName());
      Assert.assertEquals("2", param);
      return this.getClass().getName() + "-" + param;
   }
}
