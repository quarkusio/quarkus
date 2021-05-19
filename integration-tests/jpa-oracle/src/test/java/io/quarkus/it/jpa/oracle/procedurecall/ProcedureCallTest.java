package io.quarkus.it.jpa.oracle.procedurecall;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Test that procedure calls work correctly, in particular when cursors are involved.
 * <p>
 * They used to fail in native mode because adding cursor parameters involved reflection during Hibernate ORM startup,
 * and the corresponding methods were not registered for reflection.
 * See https://github.com/quarkusio/quarkus/issues/17295.
 */
@QuarkusTest
public class ProcedureCallTest {

    @Test
    public void test() {
        String response = given().param("pattern", "prefix%")
                .when().get("/jpa-oracle/procedure-call/").then()
                .statusCode(200)
                .extract().body().asString();
        assertThat(response.split("\n"))
                .containsExactlyInAnyOrder("prefix#1", "prefix#2");
    }

}
