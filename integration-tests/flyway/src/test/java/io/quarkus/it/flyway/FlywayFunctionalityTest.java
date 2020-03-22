package io.quarkus.it.flyway;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisplayName("Tests flyway extension")
public class FlywayFunctionalityTest {

    @Test
    @DisplayName("Migrates a schema correctly using integrated instance")
    public void testFlywayQuarkusFunctionality() {
        when().get("/flyway/migrate").then().body(is("1.0.1"));
    }

    @Test
    @DisplayName("Migrates a schema correctly using second instance of Flyway")
    public void testMultipleFlywayQuarkusFunctionality() {
        when().get("/flyway/multiple-flyway-migratation").then().body(is("1.0.0"));
    }

    @Test
    @DisplayName("Returns current placeholders")
    public void testPlaceholders() {
        when().get("/flyway/placeholders").then().body("foo", is("bar"));
    }

}
