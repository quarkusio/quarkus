package io.quarkus.it.kafka;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTest
public class KafkaCodecTest {

    @Test
    public void testCustomCodec() {
        RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body("{\"kind\":\"rabbit\", \"name\":\"neo\"}")
                .post("/codecs/pets");

        RestAssured
                .given()
                .header("Accept", "application/json")
                .get("/codecs/pets")
                .then()
                .body("kind", is("rabbit"))
                .body("name", is("neo"));

    }

    @Test
    public void testJsonbCodec() {
        RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body("{\"name\":\"kate\", \"id\":\"1234\"}")
                .post("/codecs/persons");

        RestAssured
                .given()
                .header("Accept", "application/json")
                .get("/codecs/persons")
                .then()
                .body("name", is("kate"))
                .body("id", is(1234));

    }

    @Test
    public void testJacksonCodec() {
        RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body("{\"title\":\"Inception\", \"year\":\"2010\"}")
                .post("/codecs/movies");

        RestAssured
                .given()
                .header("Accept", "application/json")
                .get("/codecs/movies")
                .then()
                .body("title", is("Inception"))
                .body("year", is(2010));

    }

    @Test
    public void testJsonbCodecWithList() {
        RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body("[{\"name\":\"kate\", \"id\":\"1234\"},{\"name\":\"john\", \"id\":\"2345\"}]")
                .post("/codecs/person-list");

        RestAssured
                .given()
                .header("Accept", "application/json")
                .get("/codecs/person-list")
                .then()
                .body("size()", is(2))
                .body("[0].name", is("kate"))
                .body("[0].id", is(1234))
                .body("[1].name", is("john"))
                .body("[1].id", is(2345));
        ;

    }

    @Test
    public void testJacksonCodecWithList() {
        RestAssured
                .given()
                .header("Content-Type", "application/json")
                .body("[{\"title\":\"Inception\", \"year\":\"2010\"},{\"title\":\"Terminator\", \"year\":\"1984\"}]")
                .post("/codecs/movie-list");

        RestAssured
                .given()
                .header("Accept", "application/json")
                .get("/codecs/movie-list")
                .then()
                .body("size()", is(2))
                .body("[0].title", is("Inception"))
                .body("[0].year", is(2010))
                .body("[1].title", is("Terminator"))
                .body("[1].year", is(1984));

    }

}
