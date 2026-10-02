package io.quarkus.hibernate.orm.sql_load_script;

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
import io.quarkus.hibernate.orm.data_management.PreexistingSchemaH2Database;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import tools.jackson.databind.JsonNode;

/**
 * Scripts configured through the deprecated "sql-load-script" property keep their historical behavior
 * when the database is reset from the Dev UI: they are only executed when Hibernate ORM creates the schema.
 */
@Tag(TestTags.DEVMODE)
public class DeprecatedSqlLoadScriptDevModeResetTestCase extends DevUIJsonRPCTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, MyEntityTestResource.class)
                    .addAsResource(new StringAsset("quarkus.datasource.db-kind=h2\n"
                            // "\" is a meta-character in property files
                            + "quarkus.datasource.jdbc.url="
                            + PreexistingSchemaH2Database.jdbcUrl("deprecated-sql-load-script-reset").replace("\\", "\\\\")
                            + "\n"
                            + "quarkus.hibernate-orm.schema-management.strategy=none\n"
                            + "quarkus.hibernate-orm.sql-load-script=load-script-test.sql\n"),
                            "application.properties")
                    .addAsResource("load-script-test.sql"));

    public DeprecatedSqlLoadScriptDevModeResetTestCase() {
        super("quarkus-datasource");
    }

    @Test
    public void scriptNotExecutedOnReset() throws Exception {
        // Not executed on start, since Hibernate ORM doesn't create the schema
        RestAssured.when().get("/my-entity/3").then().body(is("no entity"));
        RestAssured.when().get("/my-entity/add").then().body(is("MyEntity:added"));
        RestAssured.when().get("/my-entity/count").then().body(is("1"));

        JsonNode success = super.executeJsonRPCMethod("reset", Map.of("ds", "<default>"));
        assertTrue(success.asBoolean());

        // Not executed on reset either: nothing recreated the schema
        RestAssured.when().get("/my-entity/3").then().body(is("no entity"));
        RestAssured.when().get("/my-entity/count").then().body(is("1"));
    }
}
