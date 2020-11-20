package io.quarkus.it.mongodb.panache.person;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.quarkus.panache.common.Sort;

@Path("/persons/entity")
public class PersonEntityResource {
    @GET
    public List<PersonEntity> getPersons(@QueryParam("sort") String sort) {
        if (sort != null) {
            return PersonEntity.listAll(Sort.ascending(sort));
        }
        return PersonEntity.listAll();
    }

    @GET
    @Path("/search/{name}")
    public Set<PersonName> searchPersons(@PathParam("name") String name) {
        Set<PersonName> uniqueNames = new HashSet<>();
        List<PersonName> lastnames = PersonEntity.find("lastname = ?1 and status = ?2", name, Status.ALIVE)
                .project(PersonName.class)
                .list();
        lastnames.forEach(p -> uniqueNames.add(p));// this will throw if it's not the right type
        return uniqueNames;
    }

    @POST
    public Response addPerson(PersonEntity person) {
        person.persist();
        String id = person.id.toString();
        return Response.created(URI.create("/persons/entity/" + id)).build();
    }

    @POST
    @Path("/multiple")
    public void addPersons(List<PersonEntity> persons) {
        PersonEntity.persist(persons);
    }

    @PUT
    public Response updatePerson(PersonEntity person) {
        person.update();
        return Response.accepted().build();
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public Response upsertPerson(PersonEntity person) {
        person.persistOrUpdate();
        return Response.accepted().build();
    }

    @DELETE
    @Path("/{id}")
    public void deletePerson(@PathParam("id") String id) {
        PersonEntity person = PersonEntity.findById(Long.parseLong(id));
        person.delete();
    }

    @GET
    @Path("/{id}")
    public PersonEntity getPerson(@PathParam("id") String id) {
        return PersonEntity.findById(Long.parseLong(id));
    }

    @GET
    @Path("/count")
    public long countAll() {
        return PersonEntity.count();
    }

    @DELETE
    public void deleteAll() {
        PersonEntity.deleteAll();
    }

    @POST
    @Path("/rename")
    public Response rename(@QueryParam("previousName") String previousName, @QueryParam("newName") String newName) {
        PersonEntity.update("lastname", newName).where("lastname", previousName);
        return Response.ok().build();
    }
}
