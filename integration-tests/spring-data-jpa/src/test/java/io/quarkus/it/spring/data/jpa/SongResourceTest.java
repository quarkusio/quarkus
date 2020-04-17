package io.quarkus.it.spring.data.jpa;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SongResourceTest {

    @Test
    void testAll() {
        when().get("/song/all").then()
                .statusCode(200)
                .body(is("false - false / 6"));
    }

    @Test
    void testFindFirstPageWithTwoElements() {
        when().get("/song/page/0/2").then()
                .statusCode(200)
                .body(is("false - true / 2"));
    }

}
