package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BookResourceTest {

    @Test
    void testAll() {
        when().get("/book/all").then()
                .statusCode(200)
                .body(containsString("Sapiens"))
                .body(containsString("Homo Deus"))
                .body(containsString("Enlightenment Now"))
                .body(containsString("Factfulness"))
                .body(containsString("Sleepwalkers"))
                .body(containsString("The Silk Roads"));
    }

    @Test
    void testExistsById() {
        when().get("/book/exists/bid/1").then()
                .statusCode(200)
                .body(is("true"));

        when().get("/book/exists/bid/100").then()
                .statusCode(200)
                .body(is("false"));
    }

    @Test
    void testExistsByPublicationBetween() {
        when().get("/book/exists/publicationBetween/2000/2010").then()
                .statusCode(200)
                .body(is("false"));

        when().get("/book/exists/publicationBetween/2010/2012").then()
                .statusCode(200)
                .body(is("true"));

        when().get("/book/exists/publicationBetween/2017/2018").then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    void testNew() {
        when().get("/book/all").then()
                .statusCode(200)
                .body(not(containsString("Upheaval")));

        when().get("/book/new/50/Upheaval/2019").then()
                .statusCode(200)
                .body(containsString("Upheaval"));

        when().get("/book/all").then()
                .statusCode(200)
                .body(containsString("Upheaval"))
                .body(containsString("Sapiens"));
    }

    @Test
    void testByName() {
        when().get("/book/name/Sapiens").then()
                .statusCode(200)
                .body(containsString("Sapiens"));
    }

    @Test
    void testByNameContainingIgnoreCase() {
        when().get("/book/name/FUL/contains").then()
                .statusCode(200)
                .body(containsString("Factfulness"));
    }

    @Test
    void testCountByNameStartsWithIgnoreCase() {
        when().get("/book/name/t/count/like").then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    void testByYear() {
        when().get("/book/year/2012").then()
                .statusCode(200)
                .body(containsString("Sleepwalkers"));
        when().get("/book/year/2050").then()
                .statusCode(204);
    }

    @Test
    void testByNameNotFound() {
        when().get("/book/name/DoesNotExist").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    void testCountAllByPublicationYear() {
        when().get("/book/count/year").then()
                .statusCode(200)
                .contentType("application/json")
                .body(containsString("publicationYear"));
    }

    @Test
    void testCountAllByPublicationYear2() {
        when().get("/book/count/year2").then()
                .statusCode(200)
                .contentType("application/json")
                .body(containsString("publicationYear"));
    }

    @Test
    void testCustomFindPublicationYearPrimitive() {
        when().get("/book/customPublicationYearPrimitive/1").then()
                .statusCode(200)
                .body(is("2011"));
    }

    @Test
    void testCustomFindPublicationYearObject() {
        when().get("/book/customPublicationYearObject/1").then()
                .statusCode(200)
                .body(is("2011"));
    }
}
