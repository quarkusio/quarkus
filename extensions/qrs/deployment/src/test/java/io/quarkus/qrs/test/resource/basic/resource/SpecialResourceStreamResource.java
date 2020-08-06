package io.quarkus.qrs.test.resource.basic.resource;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.IOException;
import java.io.InputStream;

@Path("/inputstream")
public class SpecialResourceStreamResource {
   @POST
   @Path("/test/{type}")
   public void test(InputStream is, @PathParam("type") final String type) throws IOException {

   }
}
