package io.quarkus.hibernate.orm.schema_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * An explicit schema init script replaces the default one (import.sql)
 * and is executed right after Hibernate ORM created the schema.
 */
public class ExplicitSchemaInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("schema-init.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.init-script", "schema-init.sql");

    @Test
    public void explicitSchemaInitScriptExecuted() {
        RestAssured.when().get("/orm-init-script/20").then()
                .body(Matchers.is("schema init script entity"));
    }

    @Test
    public void defaultSchemaInitScriptNotExecuted() {
        RestAssured.when().get("/orm-init-script/1").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));
    }
}
