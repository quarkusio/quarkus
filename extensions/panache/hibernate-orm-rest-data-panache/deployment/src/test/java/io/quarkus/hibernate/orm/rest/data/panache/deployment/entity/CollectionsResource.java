package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true, paged = false, halCollectionName = "item-collections")
public interface CollectionsResource extends PanacheEntityResource<Collection, String> {
    @GET
    @Path("/name/{name}")
    default Collection findByName(@PathParam("name") String name) {
        List<Collection> collections = Collection.find("name = :name", Collections.singletonMap("name", name)).list();
        if (collections.isEmpty()) {
            return null;
        }

        return collections.get(0);
    }
}
