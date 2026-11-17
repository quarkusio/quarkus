package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class CoreUrlClassLoaderTestCase {

    @Test
    public void readsAResourceFromAJarThroughAUrlClassLoader() {
        RestAssured.when().get("/core/url-class-loader").then()
                .statusCode(200)
                .body(is("hello from the jar"));
    }
}
