package io.quarkus.it.mybatis;

import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
class MybatisTest {

    @Test
    public void test() {
        RestAssured.when().get("/mybatis/user/1").then()
                .body(is("{\"id\":1,\"name\":\"Test User1\"}"));

        RestAssured.given().param("id", "5").param("name", "New User").post("/mybatis/user")
                .then().body(is("1"));

        RestAssured.when().delete("/mybatis/user/1").then()
                .body(is("1"));
    }

}
