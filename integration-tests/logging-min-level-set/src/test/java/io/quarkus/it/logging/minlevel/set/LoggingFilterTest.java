package io.quarkus.it.logging.minlevel.set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(SetRuntimeLogLevels.class)
public class LoggingFilterTest {

    @Test
    public void testFiltered() {
        given()
                .when().get("/log/filter/filtered")
                .then()
                .statusCode(200)
                .body(is("false"));
    }

    @Test
    public void testNotFiltered() {
        given()
                .when().get("/log/filter/not-filtered")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

}
