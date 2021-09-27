package io.quarkus.it.mongodb;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.it.mongodb.pojo.Pojo;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
@DisabledOnOs(OS.WINDOWS)
public class PojoResourceTest {
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
