package io.quarkus.it.mongodb.panache.reactive.person.resources;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import com.mongodb.ReadPreference;

import io.quarkus.it.mongodb.panache.person.PersonName;
import io.quarkus.it.mongodb.panache.reactive.person.ReactivePersonEntity;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

@Path("/reactive/persons/entity")
public class ReactivePersonEntityResource {
    @GET
    public Uni<List<ReactivePersonEntity>> getPersons(@QueryParam("sort") String sort) {
        if (sort != null) {
            return ReactivePersonEntity.listAll(Sort.ascending(sort));
        }
        return ReactivePersonEntity.listAll();
    }

    @GET
    @Path("/search/{name}")
    public Set<PersonName> searchPersons(@PathParam("name") String name) {
        Set<PersonName> uniqueNames = new HashSet<>();
        List<PersonName> lastnames = ReactivePersonEntity.find("lastname", name)
                .project(PersonName.class)
                .withReadPreference(ReadPreference.primaryPreferred())
                .list()
                .await().indefinitely();
        lastnames.forEach(p -> uniqueNames.add(p));// this will throw if it's not the right type
        return uniqueNames;
    }

    @POST
    public Uni<Response> addPerson(ReactivePersonEntity person) {
        return person.persist().map(v -> {
            //the ID is populated before sending it to the database
            String id = person.id.toString();
            return Response.created(URI.create("/persons/entity" + id)).build();
        });
    }

    @POST
    @Path("/multiple")
    public Uni<Void> addPersons(List<ReactivePersonEntity> persons) {
        return ReactivePersonEntity.persist(persons);
    }

    @PUT
    public Uni<Response> updatePerson(ReactivePersonEntity person) {
        return person.update().map(v -> Response.accepted().build());
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public Uni<Response> upsertPerson(ReactivePersonEntity person) {
        return person.persistOrUpdate().map(v -> Response.accepted().build());
    }

    @DELETE
    @Path("/{id}")
    public Uni<Void> deletePerson(@PathParam("id") String id) {
        return ReactivePersonEntity.findById(Long.parseLong(id)).flatMap(person -> person.delete());
    }

    @GET
    @Path("/{id}")
    public Uni<ReactivePersonEntity> getPerson(@PathParam("id") String id) {
        return ReactivePersonEntity.findById(Long.parseLong(id));
    }

    @GET
    @Path("/count")
    public Uni<Long> countAll() {
        return ReactivePersonEntity.count();
    }

    @DELETE
    public Uni<Void> deleteAll() {
        return ReactivePersonEntity.deleteAll().map(l -> null);
    }

    @POST
    @Path("/rename")
    public Uni<Response> rename(@QueryParam("previousName") String previousName, @QueryParam("newName") String newName) {
        return ReactivePersonEntity.update("lastname", newName).where("lastname", previousName)
                .map(count -> Response.ok().build());
    }
}
