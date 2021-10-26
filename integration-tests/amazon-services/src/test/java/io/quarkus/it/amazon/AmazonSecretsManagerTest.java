package io.quarkus.it.amazon;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AmazonSecretsManagerTest {

    @Test
    public void testSecretsManagerAsync() {
        RestAssured.when().get("/test/secretsmanager/async").then().body(is("Quarkus is awsome"));
    }

    @Test
    public void testSecretsManagerSync() {
        RestAssured.when().get("/test/secretsmanager/sync").then().body(is("Quarkus is awsome"));
    }
}
