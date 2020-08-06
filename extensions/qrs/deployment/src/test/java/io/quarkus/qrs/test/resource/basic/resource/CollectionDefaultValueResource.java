package io.quarkus.qrs.test.resource.basic.resource;

import org.junit.Assert;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;

@Path("/collection")
public class CollectionDefaultValueResource {
   @GET
   @Produces("text/plain")
   public String get(@QueryParam("nada") List<String> params) {
      Assert.assertNotNull("Empty list was sent like null", params);
      Assert.assertEquals("Empty list was sent not empty", 0, params.size());
      return "hello";
   }

}
