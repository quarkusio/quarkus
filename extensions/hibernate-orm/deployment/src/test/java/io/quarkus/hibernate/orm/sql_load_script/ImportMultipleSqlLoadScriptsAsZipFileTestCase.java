package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ImportMultipleSqlLoadScriptsAsZipFileTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application-multiple-load-script-files-as-zip-file-test.properties",
                            "application.properties")
                    .addAsResource("multiple-load-script-files.zip"));

    @Test
    public void testMultipleLoadScriptFilesAsZipFile() {
        String name1 = "import-1.sql load script entity";
        String name2 = "import-2.sql load script entity";

        RestAssured.when().get("/orm-sql-load-script/1").then().body(Matchers.is(name1));
        RestAssured.when().get("/orm-sql-load-script/2").then().body(Matchers.is(name2));
    }
}
