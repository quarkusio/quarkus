package io.quarkus.hibernate.orm.data_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * When Hibernate ORM creates the schema, the schema init script is executed before the data init script,
 * so that the latter can rely on database objects created by the former.
 */
public class DataInitScriptExecutedAfterSchemaInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("schema-init-create-table.sql")
                    .addAsResource("data-from-extra-table.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.init-script", "schema-init-create-table.sql")
            .overrideConfigKey("quarkus.hibernate-orm.data-management.init-script", "data-from-extra-table.sql");

    @Test
    public void dataInitScriptExecutedAfterSchemaInitScript() {
        RestAssured.when().get("/orm-init-script/30").then()
                .body(Matchers.is("data init script entity after schema init script"));
    }
}
