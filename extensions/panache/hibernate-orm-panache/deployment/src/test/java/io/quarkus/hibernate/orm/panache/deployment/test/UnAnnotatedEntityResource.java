package io.quarkus.hibernate.orm.panache.deployment.test;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("unannotatedEntity")
public class UnAnnotatedEntityResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UnAnnotatedEntity> get() {
        return UnAnnotatedEntity.listAll();
    }
}
