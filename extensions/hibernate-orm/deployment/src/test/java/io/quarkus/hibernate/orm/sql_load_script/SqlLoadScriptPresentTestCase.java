package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SqlLoadScriptPresentTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, SqlLoadScriptTestResource.class)
                    .addAsResource("application-other-load-script-test.properties", "application.properties")
                    .addAsResource("load-script-test.sql"));

    @Test
    public void testSqlLoadScriptPresentTest() {
        String name = "other-load-script sql load script entity";
        RestAssured.when().get("/orm-sql-load-script/3").then().body(Matchers.is(name));
    }
}
