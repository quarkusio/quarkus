package io.quarkus.it.jpa.mariadb;

import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class StorageEngineTest {
    @Test
    public void testDialectStoreEngines() {
        RestAssured.when().get("/offline/dialect").then().body(
                containsString("storageEngine='innodb'"));

        RestAssured.when().get("/offline2/dialect").then().body(
                containsString("storageEngine='myisam'"));
    }
}
