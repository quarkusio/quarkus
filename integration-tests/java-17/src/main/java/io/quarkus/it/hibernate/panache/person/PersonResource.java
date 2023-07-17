package io.quarkus.it.hibernate.panache.person;

import java.net.URI;
import java.util.Arrays;
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

    @GET
    @Path("hql-project")
    @Transactional
    public Response testPanacheHqlProject() {
        var mark = new Person();
        mark.firstname = "Mark";
        mark.lastname = "Mark";
        mark.persistAndFlush();

        var hqlWithoutSpace = """
                select
                    firstname,
                    lastname
                from
                    io.quarkus.it.hibernate.panache.person.Person
                where
                    firstname = ?1
                """;
        var persistedWithoutSpace = Person.find(hqlWithoutSpace, "Mark").project(PersonName.class).firstResult();

        // We need to escape the whitespace in Java otherwise the compiler removes it.
        var hqlWithSpace = """
                select\s
                    firstname,
                    lastname
                from
                    io.quarkus.it.hibernate.panache.person.Person
                where
                    firstname = ?1
                """;
        var persistedWithSpace = Person.find(hqlWithSpace, "Mark").project(PersonName.class).firstResult();

        return Response.ok(Arrays.asList(persistedWithoutSpace, persistedWithSpace)).build();
    }
}
