package io.quarkus.hibernate.orm.data_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * When the schema is managed by another tool (schema management strategy "none"),
 * the data init script (data.sql) is still executed on startup,
 * while the schema init script (import.sql) is not, since Hibernate ORM did not create the schema.
 */
public class DataInitScriptWithSchemaManagementNoneTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class, PreexistingSchemaH2Database.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    PreexistingSchemaH2Database.jdbcUrl("data-init-script-schema-none"))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none");

    @Test
    public void dataInitScriptExecuted() {
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is("data.sql data init script entity"));
    }

    @Test
    public void schemaInitScriptNotExecuted() {
        RestAssured.when().get("/orm-init-script/1").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));
    }
}
