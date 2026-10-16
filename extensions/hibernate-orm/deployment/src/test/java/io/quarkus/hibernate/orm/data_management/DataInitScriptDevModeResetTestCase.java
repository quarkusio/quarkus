package io.quarkus.hibernate.orm.data_management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;

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
 * When Hibernate ORM manages the schema, resetting the database from the Dev UI recreates the schema
 * and executes the data init script (data.sql) once.
 */
@Tag(TestTags.DEVMODE)
public class DataInitScriptDevModeResetTestCase extends DevUIJsonRPCTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, MyEntityTestResource.class)
                    .addAsResource("data.sql"))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue());

    public DataInitScriptDevModeResetTestCase() {
        super("quarkus-datasource");
    }

    @Test
    public void dataInitScriptExecutedOnceOnReset() throws Exception {
        RestAssured.when().get("/my-entity/10").then().body(is("MyEntity:data.sql data init script entity"));
        RestAssured.when().get("/my-entity/add").then().body(is("MyEntity:added"));
        RestAssured.when().get("/my-entity/count").then().body(is("2"));

        JsonNode success = super.executeJsonRPCMethod("reset", Map.of("ds", "<default>"));
        assertTrue(success.asBoolean());

        RestAssured.when().get("/my-entity/10").then().body(is("MyEntity:data.sql data init script entity"));
        RestAssured.when().get("/my-entity/count").then().body(is("1"));
        // Executing the script a second time would have failed on the primary key
        assertThat(TEST.getLogRecords()).extracting(LogRecord::getMessage)
                .noneMatch(message -> message != null && message.contains("Failed to recreate schema"));
    }
}
