package io.quarkus.it.resteasy.jsonb;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/hasName")
public class HasNameResource {

    @GET
    @Produces("application/json")
    public HasName hasName() {
        return new Person("Alice", 40);
    }
}
