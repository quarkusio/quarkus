package io.quarkus.it.mongodb;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.Optional;

import jakarta.json.bind.Jsonb;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.it.mongodb.pojo.Pojo;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
@DisabledOnOs(OS.WINDOWS)
public class PojoResourceTest {

    private static Jsonb jsonb;

    @BeforeAll
    public static void giveMeAMapper() {
        jsonb = Utils.initialiseJsonb();
    }

    @AfterAll
    public static void releaseMapper() throws Exception {
        jsonb.close();
    }

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
        given().header("Content-Type", "application/json")
                .body(jsonb.toJson(pojo))
                .when().post("/pojos")
                .then().statusCode(201);

        given().get("/pojos").then().statusCode(200).body("size()", is(1));
    }
}
