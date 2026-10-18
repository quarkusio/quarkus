package io.quarkus.virtual.rr;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/itfOnClass")
public interface IResourceOnClass {

    @GET
    String testGet();

    @POST
    String testPost(String body);

}
