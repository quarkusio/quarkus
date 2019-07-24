package io.quarkus.it.resteasy.jsonb;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/cat")
public class CatResource {

    @GET
    @Produces("application/json")
    public List<Cat> cats() {
        return Collections.singletonList(new Cat("Grey", 1, "Scottish Fold"));
    }
}
