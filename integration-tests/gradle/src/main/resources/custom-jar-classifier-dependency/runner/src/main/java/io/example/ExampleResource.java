package io.example;

import io.blob.Intermediate;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class ExampleResource {

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String hello() {
    Intermediate intermediate = new Intermediate();
    intermediate.someMethod();
    return "Hello from Quarkus REST";
  }
}
