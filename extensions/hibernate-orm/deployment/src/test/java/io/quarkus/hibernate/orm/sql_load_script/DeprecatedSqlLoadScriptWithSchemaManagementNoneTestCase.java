package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.data_management.PreexistingSchemaH2Database;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Scripts configured through the deprecated "sql-load-script" property keep their historical behavior:
 * they are only executed when Hibernate ORM creates the schema.
 */
public class DeprecatedSqlLoadScriptWithSchemaManagementNoneTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class, PreexistingSchemaH2Database.class)
                    .addAsResource("application-import-load-script-test.properties", "application.properties")
                    .addAsResource("import.sql"))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    PreexistingSchemaH2Database.jdbcUrl("sql-load-script-schema-none"))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "none");

    @Test
    public void sqlLoadScriptNotExecuted() {
        RestAssured.when().get("/orm-init-script/2").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));
    }
}
