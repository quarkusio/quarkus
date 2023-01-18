package io.quarkus.it.hibernate.panache.person;

import java.net.URI;
import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/hibernate/persons")
@Transactional
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
