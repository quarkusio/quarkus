package io.quarkus.hibernate.orm.schema_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.data_management.PreexistingSchemaH2Database;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * The schema init script is only executed when Hibernate ORM creates the schema:
 * it is not executed when the schema is managed by another tool (schema management strategy "none").
 */
public class SchemaInitScriptWithSchemaManagementNoneTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("schema-init.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class, PreexistingSchemaH2Database.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    PreexistingSchemaH2Database.jdbcUrl("schema-init-script-schema-none"))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.init-script", "schema-init.sql");

    @Test
    public void schemaInitScriptNotExecuted() {
        RestAssured.when().get("/orm-init-script/20").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));
    }
}
