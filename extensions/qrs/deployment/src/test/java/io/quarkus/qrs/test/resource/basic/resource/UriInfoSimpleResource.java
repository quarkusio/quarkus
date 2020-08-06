package io.quarkus.qrs.test.resource.basic.resource;

import org.jboss.logging.Logger;
import io.quarkus.qrs.test.PortProviderUtil;
import org.junit.Assert;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.net.URI;



@Path("/")
public class UriInfoSimpleResource {
   private static Logger logger = Logger.getLogger(UriInfoSimpleResource.class);

   @Context
   UriInfo myInfo;

   @Path("/simple")
   @GET
   public String get(@Context UriInfo info, @QueryParam("abs") String abs) {
      logger.info("abs query: " + abs);
      URI base = null;
      if (abs == null) {
         base = PortProviderUtil.createURI("/", UriInfoSimpleResource.class.getSimpleName());
      } else {
         base = PortProviderUtil.createURI("/" + abs + "/", UriInfoSimpleResource.class.getSimpleName());
      }

      logger.info("BASE URI: " + info.getBaseUri());
      logger.info("Request URI: " + info.getRequestUri());
      logger.info("Absolute URI: " + info.getAbsolutePath());
      Assert.assertEquals(base.getPath(), info.getBaseUri().getPath());
      Assert.assertEquals("/simple", info.getPath());
      return "CONTENT";
   }

   @Path("/simple/fromField")
   @GET
   public String get(@QueryParam("abs") String abs) {
      logger.info("abs query: " + abs);
      URI base = null;
      if (abs == null) {
         base = PortProviderUtil.createURI("/", UriInfoSimpleResource.class.getSimpleName());
      } else {
         base = PortProviderUtil.createURI("/" + abs + "/", UriInfoSimpleResource.class.getSimpleName());
      }

      logger.info("BASE URI: " + myInfo.getBaseUri());
      logger.info("Request URI: " + myInfo.getRequestUri());
      Assert.assertEquals(base.getPath(), myInfo.getBaseUri().getPath());
      Assert.assertEquals("/simple/fromField", myInfo.getPath());
      return "CONTENT";
   }

}
