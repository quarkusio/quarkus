package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Test that import.sql is not executed when a different file is specified in quarkus.hibernate-orm.sql-load-file.
 *
 * This could happen if 'import.sql' is used for a different persistence unit, for example.
 *
 * See https://github.com/quarkusio/quarkus/issues/49075
 */
public class DefaultSqlLoadScriptUnusedTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application.properties")
                    // If both these files get executed, a constraint failure will occur due to duplicate PKs.
                    .addAsResource("import.sql")
                    .addAsResource("import.sql", "import-different-name.sql"))
            .overrideConfigKey("quarkus.hibernate-orm.sql-load-script", "import-different-name.sql")
            // Fail startup if schema management has errors (like... executing the load script multiple times)
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.halt-on-error", "true");

    @Test
    public void testImportSqlLoadScriptTest() {
        // No startup failure, so we're already good.

        // Let's just check the script _was_ executed.
        String name = "import.sql load script entity";
        RestAssured.when().get("/orm-sql-load-script/2").then().body(Matchers.is(name));
    }
}
