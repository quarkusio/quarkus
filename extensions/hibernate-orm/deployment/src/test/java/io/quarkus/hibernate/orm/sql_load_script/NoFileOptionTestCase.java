package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class NoFileOptionTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application-no-file-option-test.properties", "application.properties")
                    .addAsResource("import.sql"));

    @Test
    public void testSqlLoadScriptFileAbsentTest() {
        String name = "no entity";
        //despite the presence of import.sql, the file is not processed
        RestAssured.when().get("/orm-sql-load-script/1").then().body(Matchers.is(name));
    }
}
