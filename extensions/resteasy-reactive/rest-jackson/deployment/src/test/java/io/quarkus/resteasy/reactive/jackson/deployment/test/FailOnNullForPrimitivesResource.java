package io.quarkus.resteasy.reactive.jackson.deployment.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/fail-on-null")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FailOnNullForPrimitivesResource {

    @POST
    @Path("/primitive")
    public PrimitiveHolder testPrimitive(PrimitiveHolder holder) {
        // If name is null, set a default value to show that null boxed types are still allowed
        if (holder.getName() == null) {
            holder.setName("DEFAULT");
        }
        return holder;
    }
}
