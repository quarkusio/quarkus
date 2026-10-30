package io.quarkus.it.data.hibernate;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/cats")
public class CatResource {

    @Inject
    Cat.Repository catRepository;

    @GET
    @Path("/test-restrictions")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testRestrictions() {
        catRepository.deleteAll();

        Cat cat1 = new Cat();
        cat1.name = "Fluffy";
        cat1.age = 5;
        cat1.color = "white";
        cat1.persist();

        Cat cat2 = new Cat();
        cat2.name = "Whiskers";
        cat2.age = 3;
        cat2.color = "black";
        cat2.persist();

        Cat cat3 = new Cat();
        cat3.name = "Shadow";
        cat3.age = 7;
        cat3.color = "black";
        cat3.persist();

        List<Cat> nameResults = catRepository.find("name", "Fluffy").list();
        if (nameResults.size() != 1) {
            return "Failed: Expected 1 cat named Fluffy, found " + nameResults.size();
        }
        if (!nameResults.get(0).name.equals("Fluffy")) {
            return "Failed: Wrong cat returned";
        }

        long ageCount = catRepository.count("age > ?1", 4);
        if (ageCount != 2) {
            return "Failed: Expected 2 cats older than 4, found " + ageCount;
        }

        List<Cat> blackCats = catRepository.list("color", "black");
        if (blackCats.size() != 2) {
            return "Failed: Expected 2 black cats, found " + blackCats.size();
        }

        List<Cat> youngBlackCats = catRepository.find("color = ?1 and age < ?2", "black", 5).list();
        if (youngBlackCats.size() != 1) {
            return "Failed: Expected 1 young black cat, found " + youngBlackCats.size();
        }
        if (!youngBlackCats.get(0).name.equals("Whiskers")) {
            return "Failed: Wrong cat returned for composite query";
        }

        long orCount = catRepository.count("name = ?1 or name = ?2", "Fluffy", "Shadow");
        if (orCount != 2) {
            return "Failed: Expected 2 cats (Fluffy or Shadow), found " + orCount;
        }

        long deleted = catRepository.delete("age < ?1", 4);
        if (deleted != 1) {
            return "Failed: Expected to delete 1 cat, deleted " + deleted;
        }

        long remaining = catRepository.count();
        if (remaining != 2) {
            return "Failed: Expected 2 cats remaining, found " + remaining;
        }

        return "OK";
    }
}
