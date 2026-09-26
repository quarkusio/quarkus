package io.quarkus.hibernate.orm.data_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class MultipleDataInitScriptsAsZipFileTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class)
                    .addAsResource("application-multiple-data-init-script-files-as-zip-file-test.properties",
                            "application.properties")
                    .addAsResource("multiple-load-script-files.zip"));

    @Test
    public void testMultipleLoadScriptFilesAsZipFile() {
        String name1 = "import-1.sql load script entity";
        String name2 = "import-2.sql load script entity";

        RestAssured.when().get("/orm-init-script/1").then().body(Matchers.is(name1));
        RestAssured.when().get("/orm-init-script/2").then().body(Matchers.is(name2));
    }
}
