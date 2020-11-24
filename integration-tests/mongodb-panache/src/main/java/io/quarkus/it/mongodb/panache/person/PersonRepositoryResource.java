package io.quarkus.it.mongodb.panache.person;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.quarkus.panache.common.Sort;

@Path("/persons/repository")
public class PersonRepositoryResource {

    // fake unused injection point to force ArC to not remove this otherwise I can't mock it in the tests
    @Inject
    MockablePersonRepository mockablePersonRepository;

    @Inject
    PersonRepository personRepository;

    @GET
    public List<Person> getPersons(@QueryParam("sort") String sort) {
        if (sort != null) {
            return personRepository.listAll(Sort.ascending(sort));
        }
        return personRepository.listAll();
    }

    @GET
    @Path("/search/{name}")
    public Set<PersonName> searchPersons(@PathParam("name") String name) {
        Set<PersonName> uniqueNames = new HashSet<>();
        List<PersonName> lastnames = personRepository.find("lastname = ?1 and status = ?2", name, Status.ALIVE)
                .project(PersonName.class)
                .list();
        lastnames.forEach(p -> uniqueNames.add(p));// this will throw if it's not the right type
        return uniqueNames;
    }

    @POST
    public Response addPerson(Person person) {
        personRepository.persist(person);
        String id = person.id.toString();
        return Response.created(URI.create("/persons/repository/" + id)).build();
    }

    @POST
    @Path("/multiple")
    public void addPersons(List<Person> persons) {
        personRepository.persist(persons);
    }

    @PUT
    public Response updatePerson(Person person) {
        personRepository.update(person);
        return Response.accepted().build();
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    public Response upsertPerson(Person person) {
        personRepository.persistOrUpdate(person);
        return Response.accepted().build();
    }

    @DELETE
    @Path("/{id}")
    public void deletePerson(@PathParam("id") String id) {
        Person person = personRepository.findById(Long.parseLong(id));
        personRepository.delete(person);
    }

    @GET
    @Path("/{id}")
    public Person getPerson(@PathParam("id") String id) {
        return personRepository.findById(Long.parseLong(id));
    }

    @GET
    @Path("/count")
    public long countAll() {
        return personRepository.count();
    }

    @DELETE
    public void deleteAll() {
        personRepository.deleteAll();
    }

    @POST
    @Path("/rename")
    public Response rename(@QueryParam("previousName") String previousName, @QueryParam("newName") String newName) {
        personRepository.update("lastname", newName).where("lastname", previousName);
        return Response.ok().build();
    }
}
