package io.quarkus.hibernate.reactive.rest.data.panache.deployment.repository;

import java.util.Collections;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.hibernate.reactive.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.smallrye.mutiny.Uni;

@ResourceProperties(hal = true, paged = false, halCollectionName = "item-collections", rolesAllowed = "user")
public interface CollectionsResource extends PanacheRepositoryResource<CollectionsRepository, Collection, String> {

    @Override
    @MethodProperties(rolesAllowed = "admin")
    Uni<Boolean> delete(String name);

    @GET
    @Path("/name/{name}")
    default Uni<Collection> findByName(@PathParam("name") String name) {
        return Collection.find("name = :name", Collections.singletonMap("name", name)).singleResult();
    }

    @POST
    @Path("/name/{name}")
    default Uni<Collection> addByName(@PathParam("name") String name) {
        Collection collection = new Collection();
        collection.id = name;
        collection.name = name;
        return Collection.persist(collection).onItem().transform(res -> collection);
    }
}
