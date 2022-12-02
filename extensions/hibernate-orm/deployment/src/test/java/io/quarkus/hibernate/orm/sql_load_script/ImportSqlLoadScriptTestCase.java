package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ImportSqlLoadScriptTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application-import-load-script-test.properties", "application.properties")
                    .addAsResource("import.sql"));

    @Test
    public void testImportSqlLoadScriptTest() {
        String name = "import.sql load script entity";
        RestAssured.when().get("/orm-sql-load-script/2").then().body(Matchers.is(name));
    }
}
