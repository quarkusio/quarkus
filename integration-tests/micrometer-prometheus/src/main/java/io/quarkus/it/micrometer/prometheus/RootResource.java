package io.quarkus.it.micrometer.prometheus;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/root")
public class RootResource {

    @Path("{rootParam}/sub")
    public SubResource subResource(@PathParam("rootParam") String value) {
        return new SubResource(value);
    }
}
