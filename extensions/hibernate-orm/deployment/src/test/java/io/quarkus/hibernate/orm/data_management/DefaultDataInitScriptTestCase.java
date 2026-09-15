package io.quarkus.hibernate.orm.data_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * With the default schema management strategy in tests (drop-and-create),
 * both the default schema init script (import.sql) and the default data init script (data.sql) are executed.
 */
public class DefaultDataInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addAsResource("data.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class));

    @Test
    public void schemaInitScriptExecuted() {
        RestAssured.when().get("/orm-init-script/1").then()
                .body(Matchers.is("default sql load script entity"));
    }

    @Test
    public void dataInitScriptExecuted() {
        RestAssured.when().get("/orm-init-script/10").then()
                .body(Matchers.is("data.sql data init script entity"));
    }
}
