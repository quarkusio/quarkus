package io.quarkus.it.mongodb.panache.axle.person;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.panache.common.Sort;

@Path("/axle/persons/entity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReactivePersonEntityResource {
    @GET
    public CompletionStage<List<ReactivePersonEntity>> getPersons(@QueryParam("sort") String sort) {
        if (sort != null) {
            return ReactivePersonEntity.listAll(Sort.ascending(sort));
        }
        return ReactivePersonEntity.listAll();
    }

    @POST
    public CompletionStage<Response> addPerson(ReactivePersonEntity person) {
        return person.persist().thenApply(v -> {
            //the ID is populated before sending it to the database
            String id = person.id.toString();
            return Response.created(URI.create("/persons/entity" + id)).build();
        });
    }

    @POST
    @Path("/multiple")
    public CompletionStage<Void> addPersons(List<ReactivePersonEntity> persons) {
        return ReactivePersonEntity.persist(persons);
    }

    @PUT
    public CompletionStage<Response> updatePerson(ReactivePersonEntity person) {
        return person.update().thenApply(v -> Response.accepted().build());
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public CompletionStage<Response> upsertPerson(ReactivePersonEntity person) {
        return person.persistOrUpdate().thenApply(v -> Response.accepted().build());
    }

    @DELETE
    @Path("/{id}")
    public CompletionStage<Void> deletePerson(@PathParam("id") String id) {
        return ReactivePersonEntity.findById(Long.parseLong(id)).thenCompose(person -> person.delete());
    }

    @GET
    @Path("/{id}")
    public CompletionStage<ReactivePersonEntity> getPerson(@PathParam("id") String id) {
        return ReactivePersonEntity.findById(Long.parseLong(id));
    }

    @GET
    @Path("/count")
    public CompletionStage<Long> countAll() {
        return ReactivePersonEntity.count();
    }

    @DELETE
    public CompletionStage<Void> deleteAll() {
        return ReactivePersonEntity.deleteAll().thenAccept(l -> {
        });
    }
}
