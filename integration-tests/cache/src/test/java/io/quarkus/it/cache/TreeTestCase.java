package io.quarkus.it.cache;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@DisplayName("Tests the integration between panache and the cache extension")
public class TreeTestCase {

    @Test
    public void test() {

        // First, let's check that the import.sql file was correctly loaded.
        given().when().get("/trees").then().statusCode(200).body(containsString("Oak"), containsString("Chestnut"));

        // Then, we get one specific tree. The call result is cached because of the @CacheResult annotation.
        given().when().get("/trees/1").then().statusCode(200).body(containsString("Oak"));

        // The same tree is deleted from the database.
        given().when().delete("/trees/1").then().statusCode(204);

        // Is it really gone? Let's check.
        given().when().get("/trees").then().statusCode(200).body(not(containsString("Oak")), containsString("Chestnut"));

        // If we try to get the same tree again, it is still returned because it was cached earlier.
        given().when().get("/trees/1").then().statusCode(200).statusCode(200).body(containsString("Oak"));
    }
}
