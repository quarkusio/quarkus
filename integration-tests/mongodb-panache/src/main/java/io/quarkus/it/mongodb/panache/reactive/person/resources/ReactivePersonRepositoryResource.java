package io.quarkus.it.mongodb.panache.reactive.person.resources;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import com.mongodb.ReadPreference;

import io.quarkus.it.mongodb.panache.person.Person;
import io.quarkus.it.mongodb.panache.person.PersonName;
import io.quarkus.it.mongodb.panache.reactive.person.ReactivePersonRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

@Path("/reactive/persons/repository")
public class ReactivePersonRepositoryResource {

    @Inject
    ReactivePersonRepository reactivePersonRepository;

    @GET
    public Uni<List<Person>> getPersons(@QueryParam("sort") String sort) {
        if (sort != null) {
            return reactivePersonRepository.listAll(Sort.ascending(sort));
        }
        return reactivePersonRepository.listAll();
    }

    @GET
    @Path("/search/{name}")
    public Set<PersonName> searchPersons(@PathParam("name") String name) {
        Set<PersonName> uniqueNames = new HashSet<>();
        List<PersonName> lastnames = reactivePersonRepository.find("lastname", name)
                .project(PersonName.class)
                .withReadPreference(ReadPreference.primaryPreferred())
                .list()
                .await().indefinitely();
        lastnames.forEach(p -> uniqueNames.add(p));// this will throw if it's not the right type
        return uniqueNames;
    }

    @POST
    public Uni<Response> addPerson(Person person) {
        return reactivePersonRepository.persist(person).map(v -> {
            //the ID is populated before sending it to the database
            String id = person.id.toString();
            return Response.created(URI.create("/persons/entity" + id)).build();
        });
    }

    @POST
    @Path("/multiple")
    public Uni<Void> addPersons(List<Person> persons) {
        return reactivePersonRepository.persist(persons);
    }

    @PUT
    public Uni<Response> updatePerson(Person person) {
        return reactivePersonRepository.update(person).map(v -> Response.accepted().build());
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public Uni<Response> upsertPerson(Person person) {
        return reactivePersonRepository.persistOrUpdate(person).map(v -> Response.accepted().build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Void> deletePerson(@PathParam("id") String id) {
        return reactivePersonRepository.findById(Long.parseLong(id))
                .flatMap(person -> reactivePersonRepository.delete(person));
    }

    @GET
    @Path("/{id}")
    public Uni<Person> getPerson(@PathParam("id") String id) {
        return reactivePersonRepository.findById(Long.parseLong(id));
    }

    @GET
    @Path("/count")
    public Uni<Long> countAll() {
        return reactivePersonRepository.count();
    }

    @DELETE
    public Uni<Void> deleteAll() {
        return reactivePersonRepository.deleteAll().map(l -> null);
    }

    @POST
    @Path("/rename")
    public Uni<Response> rename(@QueryParam("previousName") String previousName, @QueryParam("newName") String newName) {
        return reactivePersonRepository.update("lastname", newName).where("lastname", previousName)
                .map(count -> Response.ok().build());
    }
}
