package io.quarkus.qrs.test.resource.basic.resource;

import org.jboss.logging.Logger;
import io.quarkus.qrs.test.TestPortProvider;
import org.junit.Assert;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

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
         base = TestPortProvider.createURI("/");
      } else {
         base = TestPortProvider.createURI("/" + abs + "/");
      }

      logger.info("BASE URI: " + myInfo.getBaseUri());
      logger.info("Request URI: " + myInfo.getRequestUri());
      Assert.assertEquals("The injected base path doesn't match to the expected one",
            base.getPath() + "ConstructedInjectionTest/", myInfo.getBaseUri().getPath());
      Assert.assertEquals("The injected path doesn't match to the expected one", "/simple", myInfo.getPath());
      return "CONTENT";
   }

}
