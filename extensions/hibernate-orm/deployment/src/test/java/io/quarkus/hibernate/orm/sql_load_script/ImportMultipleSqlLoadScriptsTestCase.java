package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ImportMultipleSqlLoadScriptsTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application-import-multiple-load-scripts-test.properties", "application.properties")
                    .addAsResource("import-multiple-load-scripts-1.sql", "import-1.sql")
                    .addAsResource("import-multiple-load-scripts-2.sql", "import-2.sql"));

    @Test
    public void testImportMultipleSqlLoadScriptsTest() {
        String name1 = "import-1.sql load script entity";
        String name2 = "import-2.sql load script entity";

        RestAssured.when().get("/orm-sql-load-script/1").then().body(Matchers.is(name1));
        RestAssured.when().get("/orm-sql-load-script/2").then().body(Matchers.is(name2));
    }
}
