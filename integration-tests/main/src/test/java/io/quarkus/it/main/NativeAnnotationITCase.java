package io.quarkus.it.main;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.NativeImageTest;
import io.restassured.RestAssured;

@NativeImageTest
public class NativeAnnotationITCase {

    @Test
    public void testClassReInitialization() {
        RestAssured.when().get("/class-reinit").then()
                .statusCode(200);
    }

    @Test
    public void testClassInitialization() {
        RestAssured.when().get("/class-init").then()
                .statusCode(200);
    }

    @Test
    public void testProxyUse() {
        RestAssured.when().get("/native-proxy").then()
                .statusCode(200)
                .body(Matchers.equalTo("passed"));
    }
}
