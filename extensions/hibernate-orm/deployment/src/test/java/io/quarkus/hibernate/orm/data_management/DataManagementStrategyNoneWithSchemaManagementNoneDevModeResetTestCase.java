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
 * With the "none" data management strategy, resetting the database from the Dev UI loads the data init script
 * once the schema has been recreated, but when neither Hibernate ORM nor a migration tool (Flyway, Liquibase)
 * manages the schema, nothing is recreated, so the data is left untouched.
 */
@Tag(TestTags.DEVMODE)
public class DataManagementStrategyNoneWithSchemaManagementNoneDevModeResetTestCase extends DevUIJsonRPCTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, MyEntityTestResource.class)
                    .addAsResource(new StringAsset("quarkus.datasource.db-kind=h2\n"
                            // "\" is a meta-character in property files
                            + "quarkus.datasource.jdbc.url="
                            + PreexistingSchemaH2Database.jdbcUrl("data-strategy-none-schema-none-reset").replace("\\", "\\\\")
                            + "\n"
                            + "quarkus.hibernate-orm.schema-management.strategy=none\n"
                            + "quarkus.hibernate-orm.data-management.strategy=none\n"),
                            "application.properties")
                    .addAsResource("data.sql"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue());

    public DataManagementStrategyNoneWithSchemaManagementNoneDevModeResetTestCase() {
        super("quarkus-datasource");
    }

    @Test
    public void dataInitScriptNotExecutedOnResetWhenNothingResetsTheSchema() throws Exception {
        // Not executed on start
        RestAssured.when().get("/my-entity/10").then().body(is("no entity"));
        RestAssured.when().get("/my-entity/add").then().body(is("MyEntity:added"));
        RestAssured.when().get("/my-entity/count").then().body(is("1"));

        JsonNode success = super.executeJsonRPCMethod("reset", Map.of("ds", "<default>"));
        assertTrue(success.asBoolean());

        // Nothing reset the schema or the data, so the data init script must not be executed on top of the existing data
        RestAssured.when().get("/my-entity/10").then().body(is("no entity"));
        RestAssured.when().get("/my-entity/count").then().body(is("1"));
        assertThat(TEST.getLogRecords()).extracting(LogRecord::getMessage)
                .noneMatch(message -> message != null && message.startsWith("Failed to"));
    }
}
