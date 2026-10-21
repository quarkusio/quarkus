package io.quarkus.it.data.hibernate;

import java.util.List;

import jakarta.data.constraint.EqualTo;
import jakarta.data.constraint.GreaterThan;
import jakarta.data.constraint.LessThan;
import jakarta.data.restrict.BasicRestriction;
import jakarta.data.restrict.Restrict;
import jakarta.data.restrict.Restriction;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/restrictions-test")
public class RestrictionTestResource {

    @Inject
    Cat.Repository catRepository;

    @GET
    @Path("/actual-restrictions")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testActualRestrictions() {
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

        Restriction<Cat> nameRestriction = BasicRestriction.of(CatData.name, EqualTo.value("Fluffy"));
        List<Cat> results = catRepository.find(nameRestriction).list();
        if (results.size() != 1) {
            return "Failed: Expected 1 cat named Fluffy, found " + results.size();
        }

        Restriction<Cat> ageRestriction = BasicRestriction.of(CatData.age, GreaterThan.bound(4));
        long count = catRepository.count(ageRestriction);
        if (count != 2) {
            return "Failed: Expected 2 cats older than 4, found " + count;
        }

        Restriction<Cat> composite = Restrict.all(
                BasicRestriction.of(CatData.color, EqualTo.value("black")),
                BasicRestriction.of(CatData.age, LessThan.bound(5)));
        List<Cat> youngBlackCats = catRepository.list(composite);
        if (youngBlackCats.size() != 1) {
            return "Failed: Expected 1 young black cat, found " + youngBlackCats.size();
        }

        return "OK";
    }
}
