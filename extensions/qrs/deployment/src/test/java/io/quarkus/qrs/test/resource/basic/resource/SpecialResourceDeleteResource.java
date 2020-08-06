package io.quarkus.qrs.test.resource.basic.resource;

import org.junit.Assert;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;

@Path("/delete")
public class SpecialResourceDeleteResource {
   @DELETE
   @Consumes("text/plain")
   public void delete(String msg) {
      Assert.assertEquals("Wrong request content", "hello", msg);
   }
}
