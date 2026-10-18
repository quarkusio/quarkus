package io.quarkus.it.flyway;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisplayName("Tests flyway extension")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Our Windows CI does not have Docker installed properly")
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
    @DisplayName("Returns whether the init-sql is CREATE SCHEMA IF NOT EXISTS TEST_SCHEMA;CREATE OR REPLACE FUNCTION TEST_SCHEMA.f_my_constant() RETURNS integer LANGUAGE plpgsql as $func$ BEGIN return 100; END $func$; or not")
    public void testReturnInitSql() {
        when().get("/flyway/init-sql").then().body(is(
                "CREATE SCHEMA IF NOT EXISTS TEST_SCHEMA;CREATE OR REPLACE FUNCTION TEST_SCHEMA.f_my_constant() RETURNS integer LANGUAGE plpgsql as $func$ BEGIN return 100; END $func$;"));
    }

    @Test
    @DisplayName("Returns whether the init-sql executed")
    public void testInitSqlExecuted() {
        when().get("/flyway/init-sql-result").then().body(is("100"));
    }

    @Test
    @DisplayName("Returns if the created-by user is scott")
    public void testCreatedByUserIsDifferent() {
        when().get("/flyway/created-by").then()
                .log().ifValidationFails()
                .body(is("scott"));
    }

}
