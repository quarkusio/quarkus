package io.quarkus.it.mongodb.panache.person;

import java.net.URI;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/mongo/persons")
public class PersonResource {
    @GET
    public List<PersonName> getPersons() {
        return Person.findAll().project(PersonName.class).list();
    }

    @POST
    public Response addPerson(Person person) {
        person.persist();
        String id = person.id.toString();
        return Response.created(URI.create("/persons/entity/" + id)).build();
    }
}
