package io.quarkus.hibernate.orm.data_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Setting the data init script to "no-file" disables the default one (data.sql).
 */
public class NoFileDataInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-orm.data-management.init-script", "no-file");

    @Test
    public void dataInitScriptNotExecuted() {
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));
    }

    @Test
    public void schemaInitScriptExecuted() {
        RestAssured.when().get("/orm-init-script/1").then()
                .body(Matchers.is("default sql load script entity"));
    }
}
