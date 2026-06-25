package io.quarkus.it.flyway.mongodb;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(FlywayMongodbTestResource.class)
@DisplayName("Tests flyway-mongodb extension")
@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Our Windows CI does not have Docker installed properly")
public class FlywayMongodbResourceTest {

    @Test
    @DisplayName("Migrates the default client at startup and inserts the expected documents")
    public void testMigrationApplied() {
        given().get("/flyway-mongodb/count")
                .then()
                .statusCode(200)
                .body(is("2"));
    }

    @Test
    @DisplayName("Substitutes placeholders in migration scripts using the default prefix/suffix")
    public void testPlaceholderSubstitution() {
        given().get("/flyway-mongodb/placeholder-color")
                .then()
                .statusCode(200)
                .body(is("orange"));
    }

    @Test
    @DisplayName("Records applied migrations in the schema history collection")
    public void testSchemaHistoryExists() {
        given().get("/flyway-mongodb/history-count")
                .then()
                .statusCode(200)
                .body(greaterThanOrEqualTo("2"));
    }

    @Test
    @DisplayName("Migrates a named MongoDB client independently of the default client")
    public void testNamedClientMigration() {
        given().get("/flyway-mongodb/secondary/count")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    @DisplayName("Migrates a second named MongoDB client (users)")
    public void testUsersClientMigration() {
        given().get("/flyway-mongodb/users/count")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    @DisplayName("Honors the per-client `collection` override for the schema history table name")
    public void testCustomHistoryTableName() {
        given().get("/flyway-mongodb/users/history-count")
                .then()
                .statusCode(200)
                .body(greaterThanOrEqualTo("1"));
    }

    @Test
    @DisplayName("Applies a Mongo index created by a migration script")
    public void testIndexCreated() {
        given().get("/flyway-mongodb/users/index-exists")
                .then()
                .statusCode(200)
                .body(is("true"));
    }

    @Test
    @DisplayName("Executes the beforeMigrate callback exactly once per migrate command")
    public void testScriptCallback() {
        given().get("/flyway-mongodb/callback-count")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    @DisplayName("Executes the beforeEachMigrate callback once per pending versioned migration")
    public void testBeforeEachMigrateCallback() {
        given().get("/flyway-mongodb/before-each-migrate-callback-count")
                .then()
                .statusCode(200)
                .body(is("2"));
    }

    @Test
    @DisplayName("Executes the afterEachMigrate callback once per pending versioned migration")
    public void testAfterEachMigrateCallback() {
        given().get("/flyway-mongodb/after-each-migrate-callback-count")
                .then()
                .statusCode(200)
                .body(is("2"));
    }

    @Test
    @DisplayName("Executes the afterMigrate callback exactly once per migrate command")
    public void testAfterMigrateCallback() {
        given().get("/flyway-mongodb/after-migrate-callback-count")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    @DisplayName("Supports programmatic migrate() for a client with migrate-at-start disabled")
    public void testManualMigration() {
        given().get("/flyway-mongodb/lazy/count")
                .then()
                .statusCode(200)
                .body(is("0"));

        given().get("/flyway-mongodb/lazy/migrate-and-count")
                .then()
                .statusCode(200)
                .body(is("1"));
    }

    @Test
    @DisplayName("Substitutes placeholders using a custom prefix/suffix configured per client")
    public void testCustomPlaceholderPrefixSuffix() {
        given().get("/flyway-mongodb/custom-ph/color")
                .then()
                .statusCode(200)
                .body(is("cyan"));
    }
}
