package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class IntroducingDefaultImportScriptTestCase {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addClasses(SqlLoadScriptTestResource.class, MyEntity.class));

    @Test
    public void testAddNewImportSql() {
        RestAssured.when().get("/orm-sql-load-script/1").then().body(Matchers.is(SqlLoadScriptTestResource.NO_ENTITY_MESSAGE));

        runner.addResourceFile("import.sql", "INSERT INTO MyEntity(id, name) VALUES(1, 'NEW SCRIPT');");

        RestAssured.when().get("/orm-sql-load-script/1").then().body(Matchers.is("NEW SCRIPT"));
    }

}
