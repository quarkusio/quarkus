package io.quarkus.hibernate.orm.data_management;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

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
 * With the "none" data management strategy, the data init script (data.sql) is not executed
 * when the database is reset from the Dev UI either, even though the reset recreates the schema:
 * the reset behaves like a start.
 */
@Tag(TestTags.DEVMODE)
public class DataManagementStrategyNoneDevModeResetTestCase extends DevUIJsonRPCTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, MyEntityTestResource.class)
                    .addAsResource(new StringAsset(
                            "quarkus.hibernate-orm.schema-management.strategy=update\n"
                                    + "quarkus.hibernate-orm.data-management.strategy=none\n"),
                            "application.properties")
                    .addAsResource("data.sql"));

    public DataManagementStrategyNoneDevModeResetTestCase() {
        super("quarkus-datasource");
    }

    @Test
    public void dataInitScriptNotExecutedOnReset() throws Exception {
        // Not executed on start
        RestAssured.when().get("/my-entity/count").then().body(is("0"));
        RestAssured.when().get("/my-entity/add").then().body(is("MyEntity:added"));
        RestAssured.when().get("/my-entity/count").then().body(is("1"));

        JsonNode success = super.executeJsonRPCMethod("reset", Map.of("ds", "<default>"));
        assertTrue(success.asBoolean());

        // The reset recreated the schema without executing the script
        RestAssured.when().get("/my-entity/count").then().body(is("0"));
    }
}
