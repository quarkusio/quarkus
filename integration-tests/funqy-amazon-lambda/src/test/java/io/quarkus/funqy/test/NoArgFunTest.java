package io.quarkus.funqy.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@ExtendWith(UseNoArgFunExtension.class)
public class NoArgFunTest {

    @Test
    public void testNoArgFun() throws Exception {
        given()
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("noArgFun"));
    }
}
