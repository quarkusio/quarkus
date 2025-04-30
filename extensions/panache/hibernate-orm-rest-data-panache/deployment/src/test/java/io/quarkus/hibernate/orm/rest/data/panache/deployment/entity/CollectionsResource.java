package io.quarkus.hibernate.orm.rest.data.panache.deployment.entity;

import java.util.Collections;
import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

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

    @Transactional
    @POST
    @Path("/name/{name}")
    default Collection addByName(@PathParam("name") String name) {
        Collection collection = new Collection();
        collection.id = name;
        collection.name = name;
        Collection.persist(collection);
        return collection;
    }
}
