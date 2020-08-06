package io.quarkus.qrs.test.resource.basic.resource;

import org.jboss.logging.Logger;
import io.quarkus.qrs.test.resource.basic.ReponseInfoTest;
import org.jboss.resteasy.util.HttpHeaderNames;
import io.quarkus.qrs.test.PortProviderUtil;
import org.junit.Assert;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URI;


@Path("/")
public class ReponseInfoResource {
   private static Logger logger = Logger.getLogger(ReponseInfoResource.class);

   @Path("/simple")
   @GET
   public String get(@QueryParam("abs") String abs) {
      logger.info("abs query: " + abs);
      URI base;
      if (abs == null) {
         base = PortProviderUtil.createURI("/new/one", ReponseInfoTest.class.getSimpleName());
      } else {
         base = PortProviderUtil.createURI("/" + abs + "/new/one", ReponseInfoTest.class.getSimpleName());
      }
      Response response = Response.temporaryRedirect(URI.create("new/one")).build();
      URI uri = (URI) response.getMetadata().getFirst(HttpHeaderNames.LOCATION);
      logger.info("Location uri: " + uri);
      Assert.assertEquals("Wrong path from URI", base.getPath(), uri.getPath());
      return "CONTENT";
   }
}
