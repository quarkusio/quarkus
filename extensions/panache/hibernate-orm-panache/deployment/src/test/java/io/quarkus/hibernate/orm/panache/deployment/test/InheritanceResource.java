package io.quarkus.hibernate.orm.panache.deployment.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.jaxrs.PathParam;

import io.quarkus.hibernate.orm.panache.deployment.test.inheritance.ChildEntity;

@Path("entity")
public class InheritanceResource {

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String get(@PathParam long id) {
        ChildEntity ret = ChildEntity.findById(id);
        if (ret == null)
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        return ret.name;
    }
}
