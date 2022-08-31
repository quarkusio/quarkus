package io.quarkus.it.mongodb;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.it.mongodb.pojo.Pojo;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
//@DisabledOnOs(OS.WINDOWS)
// This test is disabled but should only be disable on Windows (as with other MongoDB tests) due an issue at Yasson
// side https://github.com/eclipse-ee4j/yasson/issues/575.
// The Jakarata EE migration upgrades to Yasson 3.0.1 that contains the issue so we temporarily disable this test.
// To avoid forgetting it forever an issue has been created, see https://github.com/quarkusio/quarkus/issues/27619.
@Disabled
public class PojoResourceTest {

    @BeforeEach
    public void clearCollection() {
        Response response = RestAssured
                .given()
                .delete("/pojos")
                .andReturn();
        Assertions.assertEquals(200, response.statusCode());
    }

    @Test
    public void testPojoEndpoint() {
        Pojo pojo = new Pojo();
        pojo.description = "description";
        pojo.optionalString = Optional.of("optional");
        given().header("Content-Type", "application/json").body(pojo)
                .when().post("/pojos")
                .then().statusCode(201);

        given().get("/pojos").then().statusCode(200).body("size()", is(1));
    }
}
