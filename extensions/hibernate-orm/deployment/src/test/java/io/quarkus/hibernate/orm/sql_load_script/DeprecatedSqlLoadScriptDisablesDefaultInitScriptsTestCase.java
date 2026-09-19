package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Test that the default init scripts (import.sql) are not executed
 * when a different file is specified in the deprecated quarkus.hibernate-orm.sql-load-script.
 *
 * This could happen if 'import.sql' is used for a different persistence unit, for example.
 *
 * See https://github.com/quarkusio/quarkus/issues/49075
 */
public class DeprecatedSqlLoadScriptDisablesDefaultInitScriptsTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class)
                    .addAsResource("application.properties")
                    // If both these files get executed, a constraint failure will occur due to duplicate PKs.
                    .addAsResource("import.sql")
                    .addAsResource("import.sql", "import-different-name.sql"))
            .overrideConfigKey("quarkus.hibernate-orm.sql-load-script", "import-different-name.sql")
            // Fail startup if schema management has errors (like... executing the load script multiple times)
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.halt-on-error", "true");

    @Test
    public void testDefaultInitScriptsNotExecuted() {
        // No startup failure, so we're already good.

        // Let's just check the script _was_ executed.
        String name = "import.sql load script entity";
        RestAssured.when().get("/orm-init-script/2").then().body(Matchers.is(name));
    }
}
