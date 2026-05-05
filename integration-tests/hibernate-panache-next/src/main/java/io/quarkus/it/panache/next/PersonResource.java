package io.quarkus.it.panache.next;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/persons")
public class PersonResource {

    @Inject
    Person.Repository personRepository;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Person> listAll() {
        return personRepository.listAll();
    }

    @POST
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    public Person create(Person person) {
        person.persist();
        return person;
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public long count() {
        return personRepository.count();
    }

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String test() {
        // Clean up
        personRepository.deleteAll();

        // Create using instance methods (Panache Next pattern)
        Person person1 = new Person();
        person1.name = "Alice";
        person1.age = 30;
        person1.persist();

        Person person2 = new Person();
        person2.name = "Bob";
        person2.age = 25;
        person2.persist();

        // Test find using nested repository
        List<Person> persons = personRepository.find("name", "Alice").list();
        if (persons.size() != 1) {
            return "Failed: Expected 1 person named Alice, found " + persons.size();
        }

        // Test count
        long count = personRepository.count();
        if (count != 2) {
            return "Failed: Expected 2 persons, found " + count;
        }

        // Test delete
        personRepository.delete("name", "Bob");
        count = personRepository.count();
        if (count != 1) {
            return "Failed: Expected 1 person after delete, found " + count;
        }

        return "OK";
    }
}
