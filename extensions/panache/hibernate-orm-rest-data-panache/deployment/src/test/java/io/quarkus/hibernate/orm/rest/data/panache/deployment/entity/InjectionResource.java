package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
