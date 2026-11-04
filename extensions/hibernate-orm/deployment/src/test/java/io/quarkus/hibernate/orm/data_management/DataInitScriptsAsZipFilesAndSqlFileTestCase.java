package io.quarkus.hibernate.orm.data_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class DataInitScriptsAsZipFilesAndSqlFileTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class)
                    .addAsResource("load-script-test.sql")
                    .addAsResource("import-multiple-load-scripts-1.zip")
                    .addAsResource("import-multiple-load-scripts-2.zip"))
            .withConfiguration(
                    """
                            quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                            quarkus.hibernate-orm.data-management.init-script=load-script-test.sql, import-multiple-load-scripts-1.zip, import-multiple-load-scripts-2.zip
                            """);

    @Test
    public void testDataInitScriptsAsZipFilesAndSqlFile() {
        String name = "other-load-script sql load script entity";
        String name1 = "import-1.sql load script entity";
        String name2 = "import-2.sql load script entity";

        RestAssured.when().get("/orm-init-script/1").then().body(Matchers.is(name1));
        RestAssured.when().get("/orm-init-script/2").then().body(Matchers.is(name2));
        RestAssured.when().get("/orm-init-script/3").then().body(Matchers.is(name));
    }
}
