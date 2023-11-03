package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;

@Path("/call/resource")
public class InjectionResource {

    @Inject
    ItemsResource itemsResource;

    @GET
    @Path("/items")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Item> items() {
        return itemsResource.list(new Page(5), Sort.by("id"));
    }
}
