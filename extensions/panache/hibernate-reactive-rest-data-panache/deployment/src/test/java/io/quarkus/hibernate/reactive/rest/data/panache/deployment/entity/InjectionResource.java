package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

@Path("/call/resource")
public class InjectionResource {

    @Inject
    ItemsResource itemsResource;

    @GET
    @Path("/items")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<Item>> items() {
        return itemsResource.list(new Page(5), Sort.by("id"));
    }
}
