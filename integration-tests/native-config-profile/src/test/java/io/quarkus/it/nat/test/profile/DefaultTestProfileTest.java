package io.quarkus.it.nat.test.profile;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class DefaultTestProfileTest {
    @Test
    public void unusedExists() {
        RestAssured.when()
                .get("/native-config-profile/unused-exists")
                .then()
                .body(is("true"));
    }

    @Test
    public void myConfigValue() {
        RestAssured.when()
                .get("/native-config-profile/myConfigValue")
                .then()
                .body(is("foo"));
    }

}
