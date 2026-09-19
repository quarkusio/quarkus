package io.quarkus.it.data.hibernate;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/intercepted-persons")
public class InterceptedPersonResource {

    @Inject
    InterceptedPersonRepository repository;

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String test() {
        repository.deleteAll();

        Person person1 = new Person();
        person1.name = "Charlie";
        person1.age = 40;
        person1.persist();

        Person person2 = new Person();
        person2.name = "Diana";
        person2.age = 35;
        person2.persist();

        repository.flush();

        long count = repository.count();
        if (count != 2) {
            return "Failed: Expected 2 persons, found " + count;
        }

        List<Person> all = repository.listAll();
        if (all.size() != 2) {
            return "Failed: Expected 2 persons in listAll, found " + all.size();
        }

        List<Person> found = repository.find("name", "Charlie").list();
        if (found.size() != 1) {
            return "Failed: Expected 1 person named Charlie, found " + found.size();
        }

        Person charlie = found.get(0);
        Person byId = repository.findById(charlie.id);
        if (byId == null || !byId.name.equals("Charlie")) {
            return "Failed: findById did not return Charlie";
        }

        repository.delete("name", "Diana");
        count = repository.count();
        if (count != 1) {
            return "Failed: Expected 1 person after delete, found " + count;
        }

        String intercepted = repository.interceptedMethod("hello");
        if (!"hello".equals(intercepted)) {
            return "Failed: interceptedMethod did not return expected value";
        }

        return "OK";
    }
}
