package io.quarkus.hibernate.orm.data_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * An explicit data init script replaces the default one (data.sql),
 * but does not affect the default schema init script (import.sql).
 */
public class ExplicitDataInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addAsResource("data-custom.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.data-management.init-script", "data-custom.sql");

    @Test
    public void explicitDataInitScriptExecuted() {
        RestAssured.when().get("/orm-init-script/11").then()
                .body(Matchers.is("custom data init script entity"));
    }

    @Test
    public void defaultDataInitScriptNotExecuted() {
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));
    }

    @Test
    public void defaultSchemaInitScriptExecuted() {
        RestAssured.when().get("/orm-init-script/1").then()
                .body(Matchers.is("default sql load script entity"));
    }
}
