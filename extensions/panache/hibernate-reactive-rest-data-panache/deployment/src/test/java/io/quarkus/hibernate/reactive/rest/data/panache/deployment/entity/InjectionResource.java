package io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

@Path("/call/resource")
public class InjectionResource {

    @Inject
    ItemsResource itemsResource;

    @Inject
    CollectionsResource collectionsResource;

    @GET
    @Path("/items")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<Item>> items() {
        return itemsResource.list(new Page(5), Sort.by("id"));
    }

    @GET
    @Path("/collectionByName/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Collection> collectionByName(@PathParam("name") String name) {
        return collectionsResource.findByName(name);
    }
}
