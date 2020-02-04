package io.quarkus.it.mongodb.panache.axle.person;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.it.mongodb.panache.person.Person;
import io.quarkus.panache.common.Sort;

@Path("/axle/persons/repository")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReactivePersonRepositoryResource {

    @Inject
    ReactivePersonRepository reactivePersonRepository;

    @GET
    public CompletionStage<List<Person>> getPersons(@QueryParam("sort") String sort) {
        if (sort != null) {
            return reactivePersonRepository.listAll(Sort.ascending(sort));
        }
        return reactivePersonRepository.listAll();
    }

    @POST
    public CompletionStage<Response> addPerson(Person person) {
        return reactivePersonRepository.persist(person).thenApply(v -> {
            //the ID is populated before sending it to the database
            String id = person.id.toString();
            return Response.created(URI.create("/persons/entity" + id)).build();
        });
    }

    @POST
    @Path("/multiple")
    public CompletionStage<Void> addPersons(List<Person> persons) {
        return reactivePersonRepository.persist(persons);
    }

    @PUT
    public CompletionStage<Response> updatePerson(Person person) {
        return reactivePersonRepository.update(person).thenApply(v -> Response.accepted().build());
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public CompletionStage<Response> upsertPerson(Person person) {
        return reactivePersonRepository.persistOrUpdate(person).thenApply(v -> Response.accepted().build());
    }

    @DELETE
    @Path("/{id}")
    public CompletionStage<Void> deletePerson(@PathParam("id") String id) {
        return reactivePersonRepository.findById(Long.parseLong(id))
                .thenCompose(person -> reactivePersonRepository.delete(person));
    }

    @GET
    @Path("/{id}")
    public CompletionStage<Person> getPerson(@PathParam("id") String id) {
        return reactivePersonRepository.findById(Long.parseLong(id));
    }

    @GET
    @Path("/count")
    public CompletionStage<Long> countAll() {
        return reactivePersonRepository.count();
    }

    @DELETE
    public CompletionStage<Void> deleteAll() {
        return reactivePersonRepository.deleteAll().thenAccept(l -> {
        });
    }
}
