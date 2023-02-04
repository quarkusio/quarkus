package io.quarkus.flyway.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.flywaydb.core.Flyway;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class FlywayExtensionRepairAtStartTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(FlywayResource.class)
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql")
                    .addAsResource("repair-at-start-config.properties", "application.properties"))
            .setLogRecordPredicate(r -> true)
            .setAllowFailedStart(true);

    @Test
    @DisplayName("Repair at start works correctly")
    public void testRepairUsingDevMode() {
        assertThat(RestAssured.get("/flyway/current-version").then().statusCode(200).extract().asString()).isEqualTo("1.0.0");

        config.clearLogRecords();
        config.modifyResourceFile("db/migration/V1.0.0__Quarkus.sql", s -> s + "\nNONSENSE STATEMENT CHANGING CHECKSUM;");
        config.modifyResourceFile("application.properties", s -> s + "\nquarkus.flyway.validate-on-migrate=true");

        // trigger application restart
        RestAssured.get("/");

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(config.getLogRecords()).anySatisfy(r -> {
                assertThat(r.getMessage()).contains("Failed to start application");
                assertThat(r.getThrown().getMessage()).contains("Migration checksum mismatch for migration version 1.0.0");
            });
            RestAssured.get("/flyway/current-version").then().statusCode(500);
        });

        config.clearLogRecords();
        config.modifyResourceFile("application.properties", s -> s + "\nquarkus.flyway.repair-at-start=true");

        // trigger application restart
        RestAssured.get("/");

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(config.getLogRecords()).anySatisfy(
                    r -> assertThat(r.getMessage()).contains("Successfully repaired schema history table"));
            assertThat(RestAssured.get("/flyway/current-version").then().statusCode(200).extract().asString())
                    .isEqualTo("1.0.0");
        });
    }

    @Path("flyway")
    public static class FlywayResource {
        @Inject
        Flyway flyway;

        @Path("current-version")
        @GET
        public String currentVersion() {
            return flyway.info().current().getVersion().toString();
        }
    }

}
