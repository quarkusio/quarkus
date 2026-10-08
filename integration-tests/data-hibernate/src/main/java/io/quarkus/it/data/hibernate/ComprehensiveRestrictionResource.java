package io.quarkus.it.data.hibernate;

import java.util.Arrays;
import java.util.List;

import jakarta.data.constraint.AtLeast;
import jakarta.data.constraint.AtMost;
import jakarta.data.constraint.Between;
import jakarta.data.constraint.EqualTo;
import jakarta.data.constraint.GreaterThan;
import jakarta.data.constraint.In;
import jakarta.data.constraint.LessThan;
import jakarta.data.constraint.Like;
import jakarta.data.constraint.NotBetween;
import jakarta.data.constraint.NotEqualTo;
import jakarta.data.constraint.NotIn;
import jakarta.data.constraint.NotLike;
import jakarta.data.constraint.NotNull;
import jakarta.data.constraint.Null;
import jakarta.data.restrict.BasicRestriction;
import jakarta.data.restrict.Restrict;
import jakarta.data.restrict.Restriction;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/comprehensive-restrictions")
public class ComprehensiveRestrictionResource {

    @Inject
    Cat.Repository catRepository;

    @GET
    @Path("/test-all")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String testAll() {
        catRepository.deleteAll();

        Cat cat1 = new Cat();
        cat1.name = "Fluffy";
        cat1.age = 2;
        cat1.color = "white";
        cat1.persist();

        Cat cat2 = new Cat();
        cat2.name = "Whiskers";
        cat2.age = 5;
        cat2.color = "black";
        cat2.persist();

        Cat cat3 = new Cat();
        cat3.name = "Shadow";
        cat3.age = 8;
        cat3.color = "black";
        cat3.persist();

        Cat cat4 = new Cat();
        cat4.name = "Mittens";
        cat4.age = 3;
        cat4.color = "gray";
        cat4.persist();

        Cat cat5 = new Cat();
        cat5.name = null;
        cat5.age = 1;
        cat5.color = "orange";
        cat5.persist();

        String result = testEqualTo();
        if (!result.equals("OK"))
            return "testEqualTo: " + result;

        result = testNotEqualTo();
        if (!result.equals("OK"))
            return "testNotEqualTo: " + result;

        result = testGreaterThan();
        if (!result.equals("OK"))
            return "testGreaterThan: " + result;

        result = testLessThan();
        if (!result.equals("OK"))
            return "testLessThan: " + result;

        result = testAtLeast();
        if (!result.equals("OK"))
            return "testAtLeast: " + result;

        result = testAtMost();
        if (!result.equals("OK"))
            return "testAtMost: " + result;

        result = testBetween();
        if (!result.equals("OK"))
            return "testBetween: " + result;

        result = testNotBetween();
        if (!result.equals("OK"))
            return "testNotBetween: " + result;

        result = testLike();
        if (!result.equals("OK"))
            return "testLike: " + result;

        result = testNotLike();
        if (!result.equals("OK"))
            return "testNotLike: " + result;

        result = testIn();
        if (!result.equals("OK"))
            return "testIn: " + result;

        result = testNotIn();
        if (!result.equals("OK"))
            return "testNotIn: " + result;

        result = testNull();
        if (!result.equals("OK"))
            return "testNull: " + result;

        result = testNotNull();
        if (!result.equals("OK"))
            return "testNotNull: " + result;

        result = testCompositeAll();
        if (!result.equals("OK"))
            return "testCompositeAll: " + result;

        result = testCompositeAny();
        if (!result.equals("OK"))
            return "testCompositeAny: " + result;

        result = testNegate();
        if (!result.equals("OK"))
            return "testNegate: " + result;

        return "OK";
    }

    private String testEqualTo() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.name, EqualTo.value("Fluffy"));
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 1 || !results.get(0).name.equals("Fluffy")) {
            return "Failed: Expected 1 cat named Fluffy, found " + results.size();
        }
        return "OK";
    }

    private String testNotEqualTo() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.color, NotEqualTo.value("black"));
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 3) {
            return "Failed: Expected 3 non-black cats, found " + results.size();
        }
        return "OK";
    }

    private String testGreaterThan() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.age, GreaterThan.bound(5));
        long count = catRepository.count(restriction);
        if (count != 1) {
            return "Failed: Expected 1 cat older than 5, found " + count;
        }
        return "OK";
    }

    private String testLessThan() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.age, LessThan.bound(3));
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 2) {
            return "Failed: Expected 2 cats younger than 3, found " + results.size();
        }
        return "OK";
    }

    private String testAtLeast() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.age, AtLeast.min(5));
        long count = catRepository.count(restriction);
        if (count != 2) {
            return "Failed: Expected 2 cats aged 5 or more, found " + count;
        }
        return "OK";
    }

    private String testAtMost() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.age, AtMost.max(3));
        long count = catRepository.count(restriction);
        if (count != 3) {
            return "Failed: Expected 3 cats aged 3 or less, found " + count;
        }
        return "OK";
    }

    private String testBetween() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.age, Between.bounds(2, 5));
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 3) {
            return "Failed: Expected 3 cats aged between 2 and 5, found " + results.size();
        }
        return "OK";
    }

    private String testNotBetween() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.age, NotBetween.bounds(2, 5));
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 2) {
            return "Failed: Expected 2 cats not aged between 2 and 5, found " + results.size();
        }
        return "OK";
    }

    private String testLike() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.name, Like.pattern("S%"));
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 1) {
            return "Failed: Expected 1 cat with name starting with S, found " + results.size();
        }
        return "OK";
    }

    private String testNotLike() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.name, NotLike.pattern("S%"));
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 3) {
            return "Failed: Expected 3 cats with name not starting with S, found " + results.size();
        }
        return "OK";
    }

    private String testIn() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.color, In.values(Arrays.asList("white", "gray")));
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 2) {
            return "Failed: Expected 2 cats with white or gray color, found " + results.size();
        }
        return "OK";
    }

    private String testNotIn() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.color, NotIn.values(Arrays.asList("black", "white")));
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 2) {
            return "Failed: Expected 2 cats not white or black, found " + results.size();
        }
        return "OK";
    }

    private String testNull() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.name, Null.instance());
        long count = catRepository.count(restriction);
        if (count != 1) {
            return "Failed: Expected 1 cat with null name, found " + count;
        }
        return "OK";
    }

    private String testNotNull() {
        Restriction<Cat> restriction = BasicRestriction.of(CatData.name, NotNull.instance());
        long count = catRepository.count(restriction);
        if (count != 4) {
            return "Failed: Expected 4 cats with non-null name, found " + count;
        }
        return "OK";
    }

    private String testCompositeAll() {
        Restriction<Cat> restriction = Restrict.all(
                BasicRestriction.of(CatData.color, EqualTo.value("black")),
                BasicRestriction.of(CatData.age, GreaterThan.bound(5)));
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 1 || !results.get(0).name.equals("Shadow")) {
            return "Failed: Expected 1 old black cat (Shadow), found " + results.size();
        }
        return "OK";
    }

    private String testCompositeAny() {
        Restriction<Cat> restriction = Restrict.any(
                BasicRestriction.of(CatData.color, EqualTo.value("white")),
                BasicRestriction.of(CatData.age, LessThan.bound(2)));
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 2) {
            return "Failed: Expected 2 cats (white OR age < 2), found " + results.size();
        }
        return "OK";
    }

    private String testNegate() {
        Restriction<Cat> restriction = Restrict.all(
                BasicRestriction.of(CatData.color, EqualTo.value("black"))).negate();
        List<Cat> results = catRepository.list(restriction);
        if (results.size() != 3) {
            return "Failed: Expected 3 non-black cats, found " + results.size();
        }
        return "OK";
    }
}
