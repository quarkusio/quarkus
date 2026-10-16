package io.quarkus.hibernate.orm.schema_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class DefaultSchemaInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class));

    @Test
    public void testDefaultSchemaInitScript() {
        String name = "default sql load script entity";
        RestAssured.when().get("/orm-init-script/1").then().body(Matchers.is(name));
    }
}
