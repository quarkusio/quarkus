package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Scripts configured through the deprecated "sql-load-script" property are still executed when Hibernate ORM creates the
 * schema.
 */
public class DeprecatedSqlLoadScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class)
                    .addAsResource("application-import-load-script-test.properties", "application.properties")
                    .addAsResource("import.sql"));

    @Test
    public void testDeprecatedSqlLoadScriptExecuted() {
        String name = "import.sql load script entity";
        RestAssured.when().get("/orm-init-script/2").then().body(Matchers.is(name));
    }
}
