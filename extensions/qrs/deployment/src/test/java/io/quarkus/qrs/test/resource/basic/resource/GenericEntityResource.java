package io.quarkus.qrs.test.resource.basic.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/")
public class GenericEntityResource {
   @Path("floats")
   @GET
   public Response getFloats() {
      ArrayList<Float> list = new ArrayList<Float>();
      list.add(45.0f);
      list.add(50.0f);
      GenericEntity<List<Float>> ge = new GenericEntity<List<Float>>(list) {
      };
      return Response.ok(ge).build();
   }

   @Path("doubles")
   @GET
   public Response getDoubles() {
      ArrayList<Double> list = new ArrayList<Double>();
      list.add(45.0);
      list.add(50.0);
      GenericEntity<List<Double>> ge = new GenericEntity<List<Double>>(list) {
      };
      return Response.ok(ge).build();
   }
}
