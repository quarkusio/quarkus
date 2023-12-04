package io.quarkus.it.mongodb.panache.record;

import java.net.URI;
import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/mongo/persons")
public class PersonResource {
    @GET
    public List<PersonName> getPersons() {
        return PersonWithRecord.findAll().project(PersonName.class).list();
    }

    @POST
    public Response addPerson(PersonWithRecord person) {
        person.persist();
        String id = person.id.toString();
        return Response.created(URI.create("/persons/entity/" + id)).build();
    }
}
