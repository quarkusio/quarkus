package io.quarkus.hibernate.reactive.rest.data.panache.deployment.security.entity;

import java.util.Collections;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.quarkus.hibernate.reactive.rest.data.panache.PanacheEntityResource;
import io.quarkus.hibernate.reactive.rest.data.panache.deployment.entity.Collection;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.security.PermissionsAllowed;
import io.smallrye.mutiny.Uni;

@ResourceProperties(hal = true, paged = false, halCollectionName = "item-collections")
public interface CollectionsResource extends PanacheEntityResource<Collection, String> {

    @PermissionsAllowed("find-by-name-1")
    @PermissionsAllowed("find-by-name-2")
    @GET
    @Path("/name/{name}")
    default Uni<Collection> findByName(@PathParam("name") String name) {
        return Collection.find("name = :name", Collections.singletonMap("name", name)).singleResult();
    }

    @RolesAllowed("admin")
    @POST
    @Path("/name/{name}")
    default Uni<Collection> addByName(@PathParam("name") String name) {
        Collection collection = new Collection();
        collection.id = name;
        collection.name = name;
        return Collection.persist(collection).onItem().transform(res -> collection);
    }

    @PermissionsAllowed("get-1")
    @PermissionsAllowed("get-2")
    Uni<Collection> get(String id);

    @PermissionsAllowed("add")
    Uni<Collection> add(Collection entity);
}
