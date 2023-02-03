package io.quarkus.hibernate.orm.panache.deployment.test;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("unannotatedEntity")
public class UnAnnotatedEntityResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UnAnnotatedEntity> get() {
        return UnAnnotatedEntity.listAll();
    }
}
