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
        when().get("/flyway/migrate").then().body(is("1.0.2"));
    }

    @Test
    @DisplayName("Migrates a schema correctly using second instance of Flyway")
    public void testMultipleFlywayQuarkusFunctionality() {
        when().get("/flyway/multiple-flyway-migration").then().body(is("1.0.0"));
    }

    @Test
    @DisplayName("Returns current placeholders")
    public void testPlaceholders() {
        when().get("/flyway/placeholders").then().body("foo", is("bar"));
    }

    @Test
    @DisplayName("Returns whether the createSchemas flag is used or not")
    public void testCreateSchemasDefaultIsTrue() {
        when().get("/flyway/create-schemas").then().body(is("true"));
    }

    @Test
    @DisplayName("Verify placeholder replacement")
    public void testPlaceholdersPrefixSuffix() {
        when().get("/flyway/title").then().body(is("1.0.1 REPLACED"));
    }

    @Test
    @DisplayName("Returns whether the init-sql is CREATE CONSTANT IF NOT EXISTS ONE_HUNDRED VALUE 100 or not")
    public void testReturnInitSql() {
        when().get("/flyway/init-sql").then().body(is("CREATE CONSTANT IF NOT EXISTS ONE_HUNDRED VALUE 100"));
    }

    @Test
    @DisplayName("Returns whether the init-sql executed")
    public void testInitSqlExecuted() {
        when().get("/flyway/init-sql-result").then().body(is("100"));
    }

}
