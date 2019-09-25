package io.quarkus.it.mongodb.panache.person;

import java.net.URI;
import java.util.List;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.panache.common.Sort;

@Path("/persons/entity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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
    public List<PersonName> searchPersons(@PathParam("name") String name) {
        return PersonEntity.find("lastname", name).project(PersonName.class).list();
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
}
