package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CatResourceTest {

    private static final Set<String> NOT_ADDED_OR_REMOVED = new HashSet<>(
            Arrays.asList("Scottish Fold", "Persian", "Turkish Angora", "British Shorthair"));

    @Test
    void testAll() {
        List<Cat> cats = when().get("/cat/all").then()
                .statusCode(200)
                .extract().body().jsonPath().getList(".", Cat.class);

        // make sure /all contains the elements we know that are never removed
        assertThat(cats).extracting("breed").filteredOn(NOT_ADDED_OR_REMOVED::contains)
                .containsExactlyInAnyOrder(new ArrayList<>(NOT_ADDED_OR_REMOVED).toArray(new String[0]));
    }

    @Test
    void testByBreed() {
        when().get("/cat/breed/Dummy").then()
                .statusCode(204);

        when().get("/cat/breed/Persian").then()
                .statusCode(200)
                .body(containsString("Persian"))
                .body(containsString("Grey"));
    }

    @Test
    void testByOptionalByColor() {
        when().get("/cat/color/Dummy").then()
                .statusCode(200)
                .body(is("null"));

        when().get("/cat/color/White").then()
                .statusCode(200)
                .body(containsString("Turkish Angora"))
                .body(containsString("White"));

        // there are multiple Cats with Grey color so this throws an exception
        when().get("/cat/color/Grey").then()
                .statusCode(500);
    }

    @Test
    void testByColorAndBreedAllIgnoreCase() {
        when().get("/cat/by/color/Dummy/breed/Persian").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/cat/by/color/grey/breed/persian").then()
                .statusCode(200)
                .body("size()", is(1))
                .body(containsString("Persian"));
    }

    @Test
    void testByColorIgnoreCaseAndBreed() {
        when().get("/cat/by/color/Dummy/breed/Persian").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/cat/by2/color/grey/breed/persian").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/cat/by2/color/grey/breed/Persian").then()
                .statusCode(200)
                .body("size()", is(1))
                .body(containsString("Persian"));
    }

    @Test
    void testByColorIsNotNullOrderByIdDesc() {
        List<Cat> cats = when().get("/cat/color/notNull").then()
                .extract().body().jsonPath().getList(".", Cat.class);

        assertThat(cats)
                .filteredOn(c -> NOT_ADDED_OR_REMOVED.contains(c.getBreed()))
                .extracting("id").containsExactly(3L, 2L, 1L);
    }

    @Test
    void testByColorOrBreed() {
        when().get("/cat/byOr/color/Dummy/breed/Dummy").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/cat/byOr/color/Dummy/breed/Persian").then()
                .statusCode(200)
                .body("size()", is(1))
                .body(containsString("Persian"));

        when().get("/cat/byOr/color/Grey/breed/Dummy").then()
                .statusCode(200)
                .body("size()", is(2))
                .body(containsString("Persian"))
                .body(containsString("Scottish Fold"));
    }

    @Test
    void testByBreedContaining() {
        when().get("/cat/by/breed/containing/ummy").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/cat/by/breed/containing/ish").then()
                .statusCode(200)
                .body("size()", is(3))
                .body(containsString("Scottish Fold"))
                .body(containsString("British Shorthair"))
                .body(containsString("Turkish Angora"));
    }

    @Test
    void testFindByColorStartingWithOrBreedEndingWith() {
        when().get("/cat/by/color/startsWith/Dummy/breed/endsWith/Dummy").then()
                .statusCode(200)
                .body("size()", is(0));

        when().get("/cat/by/color/startsWith/Whi/breed/endsWith/Dummy").then()
                .statusCode(200)
                .body("size()", is(1))
                .body(containsString("Turkish Angora"));

        when().get("/cat/by/color/startsWith/Dumm/breed/endsWith/old").then()
                .statusCode(200)
                .body("size()", is(1))
                .body(containsString("Scottish Fold"));

        when().get("/cat/by/color/startsWith/Gr/breed/endsWith/ora").then()
                .statusCode(200)
                .body("size()", is(3))
                .body(containsString("Scottish Fold"))
                .body(containsString("Persian"))
                .body(containsString("Turkish Angora"));
    }

    @Test
    void testCountByColor() {
        when().get("/cat/count/by/color/Dummy").then()
                .statusCode(200)
                .body(is("0"));

        when().get("/cat/count/by/color/White").then()
                .statusCode(200)
                .body(is("1"));

        when().get("/cat/count/by/color/Grey").then()
                .statusCode(200)
                .body(is("2"));
    }

    @Test
    void testExistsByColorStartsWith() {
        when().get("/cat/exists/by/colorStartsWith/Dum").then()
                .statusCode(200)
                .body(is("false"));

        when().get("/cat/exists/by/colorStartsWith/Gr").then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void testDeleteByColor() {
        when().get("/cat/all").then()
                .statusCode(200)
                .body(containsString("Bombay Cat"));

        when().get("/cat/delete/by/color/Black").then()
                .statusCode(204);

        when().get("/cat/all").then()
                .statusCode(200)
                .body(not(containsString("Bombay Cat")))
                .body(containsString("Persian"));
    }

    @Test
    void testFindCatsByBreedIn() {
        when().get("/cat/findCatsByBreedIsIn").then()
                .statusCode(200)
                .body("size()", is(2))
                .body(containsString("Persian"))
                .body(containsString("British Shorthair"));
    }

    @Test
    void testByDistinctiveFalse() {
        when().get("/cat/by/distinctive/false").then()
                .statusCode(200)
                .body(containsString("British Shorthair"))
                .body(not(containsString("Persian")));
    }
}
