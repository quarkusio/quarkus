package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Setting the deprecated "sql-load-script" property to "no-file" also disables the default init scripts (import.sql).
 */
public class DeprecatedSqlLoadScriptNoFileTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class)
                    .addAsResource("application-no-file-option-test.properties", "application.properties")
                    .addAsResource("import.sql"));

    @Test
    public void testDefaultInitScriptsNotExecuted() {
        String name = "no entity";
        //despite the presence of import.sql, the file is not processed
        RestAssured.when().get("/orm-init-script/1").then().body(Matchers.is(name));
    }
}
