package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.MyEntityTestResource;
import io.quarkus.hibernate.orm.TestTags;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import tools.jackson.databind.JsonNode;

/**
 * Resetting the database from the Dev UI does not execute the data init script again
 * when neither Hibernate ORM nor a migration tool (Flyway, Liquibase) resets the schema:
 * the data is still there, and executing the script again would apply it on top of it.
 */
@Tag(TestTags.DEVMODE)
public class DataInitScriptWithSchemaManagementNoneDevModeResetTestCase extends DevUIJsonRPCTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, MyEntityTestResource.class)
                    .addAsResource(new StringAsset("quarkus.datasource.db-kind=h2\n"
                            // "\" is a meta-character in property files
                            + "quarkus.datasource.jdbc.url="
                            + PreexistingSchemaH2Database.jdbcUrl("data-init-script-schema-none-reset").replace("\\", "\\\\")
                            + "\n"
                            + "quarkus.hibernate-orm.schema-management.strategy=none\n"),
                            "application.properties")
                    .addAsResource("data.sql"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue());

    public DataInitScriptWithSchemaManagementNoneDevModeResetTestCase() {
        super("quarkus-datasource");
    }

    @Test
    public void dataInitScriptNotExecutedAgainOnReset() throws Exception {
        // Executed on start, on the schema that already exists
        RestAssured.when().get("/my-entity/10").then().body(is("MyEntity:data.sql data init script entity"));
        RestAssured.when().get("/my-entity/add").then().body(is("MyEntity:added"));
        RestAssured.when().get("/my-entity/count").then().body(is("2"));

        JsonNode success = super.executeJsonRPCMethod("reset", Map.of("ds", "<default>"));
        assertTrue(success.asBoolean());

        // Nothing reset the schema or the data, so the data init script must not be executed again
        RestAssured.when().get("/my-entity/10").then().body(is("MyEntity:data.sql data init script entity"));
        RestAssured.when().get("/my-entity/count").then().body(is("2"));
        // Executing the script again would have failed on the primary key
        assertThat(TEST.getLogRecords()).extracting(LogRecord::getMessage)
                .noneMatch(message -> message != null && message.startsWith("Failed to"));
    }
}
