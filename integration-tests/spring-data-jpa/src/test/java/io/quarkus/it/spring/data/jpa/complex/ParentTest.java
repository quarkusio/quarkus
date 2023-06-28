package io.quarkus.it.spring.data.jpa.complex;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ParentTest {

    @Test
    @Order(1)
    void noEntities() {
        when().get("/complex/parents/p1/1")
                .then()
                .statusCode(204);
        when().get("/complex/parents/p2/1")
                .then()
                .statusCode(204);
    }

    @Test
    @Order(2)
    void createParent() {
        given().accept(ContentType.JSON).when().post("/complex/parents/1")
                .then()
                .statusCode(200)
                .body("id", is(1));
    }

    @Test
    @Order(3)
    void existingEntities() {
        when().get("/complex/parents/p1/1?name=Test")
                .then()
                .statusCode(200)
                .body("age", is(50));
        when().get("/complex/parents/p2/1?name=Test")
                .then()
                .statusCode(200)
                .body("age", is(50));
    }
}
