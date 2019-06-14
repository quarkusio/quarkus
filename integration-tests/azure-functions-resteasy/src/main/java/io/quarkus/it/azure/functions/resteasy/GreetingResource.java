package io.quarkus.it.azure.functions.resteasy;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("hello")
public class GreetingResource {
    @GET
    @Produces("text/plain")
    public String get(@QueryParam("name") String name) {
        return "Hello " + ((name == null) ? "nobody" : name);
    }

    @POST
    @Consumes("text/plain")
    @Produces("text/plain")
    public String post(String name) {
        return "Hello " + ((name == null) ? "nobody" : name);
    }
}
