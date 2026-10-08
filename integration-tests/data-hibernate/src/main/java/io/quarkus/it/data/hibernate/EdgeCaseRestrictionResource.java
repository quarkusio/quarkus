package io.quarkus.it.data.hibernate;

import java.util.List;

import jakarta.data.constraint.EqualTo;
import jakarta.data.constraint.GreaterThan;
import jakarta.data.constraint.NotNull;
import jakarta.data.restrict.BasicRestriction;
import jakarta.data.restrict.Restrict;
import jakarta.data.restrict.Restriction;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/edge-case-restrictions")
public class EdgeCaseRestrictionResource {

    @Inject
    Cat.Repository catRepository;

    @GET
    @Path("/test-edge-cases")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testEdgeCases() {
        catRepository.deleteAll();

        Cat cat1 = new Cat();
        cat1.name = "Test1";
        cat1.age = 5;
        cat1.color = "black";
        cat1.persist();

        Cat cat2 = new Cat();
        cat2.name = null;
        cat2.age = 3;
        cat2.color = "white";
        cat2.persist();

        String result = testSingleRestrictionComposite();
        if (!result.equals("OK"))
            return "testSingleRestrictionComposite: " + result;

        result = testNestedComposite();
        if (!result.equals("OK"))
            return "testNestedComposite: " + result;

        result = testNegatedComposite();
        if (!result.equals("OK"))
            return "testNegatedComposite: " + result;

        result = testMultiLevelNesting();
        if (!result.equals("OK"))
            return "testMultiLevelNesting: " + result;

        return "OK";
    }

    private String testSingleRestrictionComposite() {
        Restriction<Cat> restriction = Restrict.all(
                BasicRestriction.of(CatData.name, NotNull.instance()));
        long count = catRepository.count(restriction);
        if (count != 1) {
            return "Failed: Expected 1 cat with non-null name, found " + count;
        }
        return "OK";
    }

    private String testNestedComposite() {
        Restriction<Cat> restriction = Restrict.all(
                Restrict.any(
                        BasicRestriction.of(CatData.color, EqualTo.value("black")),
                        BasicRestriction.of(CatData.color, EqualTo.value("white"))),
                BasicRestriction.of(CatData.age, GreaterThan.bound(2)));
        long count = catRepository.count(restriction);
        if (count != 2) {
            return "Failed: Expected 2 cats matching nested restriction, found " + count;
        }
        return "OK";
    }

    private String testNegatedComposite() {
        Restriction<Cat> restriction = Restrict.all(
                BasicRestriction.of(CatData.color, EqualTo.value("black"))).negate();
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 1) {
            return "Failed: Expected 1 non-black cat, found " + results.size();
        }
        return "OK";
    }

    private String testMultiLevelNesting() {
        Restriction<Cat> restriction = Restrict.all(
                Restrict.any(
                        Restrict.all(
                                BasicRestriction.of(CatData.color, EqualTo.value("black")),
                                BasicRestriction.of(CatData.age, GreaterThan.bound(4))),
                        BasicRestriction.of(CatData.name, NotNull.instance())));
        long count = catRepository.count(restriction);
        if (count != 1) {
            return "Failed: Expected 1 cat matching multi-level nested restriction, found " + count;
        }
        return "OK";
    }
}
