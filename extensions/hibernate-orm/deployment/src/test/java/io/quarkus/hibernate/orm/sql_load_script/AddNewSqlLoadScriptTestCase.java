package io.quarkus.hibernate.orm.sql_load_script;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class AddNewSqlLoadScriptTestCase {
    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("import.sql")
                    .addClasses(SqlLoadScriptTestResource.class, MyEntity.class));

    @Test
    public void testAddNewImportSql() {
        String name = "default sql load script entity";
        RestAssured.when().get("/orm-sql-load-script/1").then().body(Matchers.is(name));

        runner.modifyResourceFile("application.properties",
                (s) -> s += "\nquarkus.hibernate-orm.sql-load-script=new-load-script-test.sql");
        runner.addResourceFile("new-load-script-test.sql", "INSERT INTO MyEntity(id, name) VALUES(1, 'NEW SCRIPT');");
        RestAssured.when().get("/orm-sql-load-script/1").then().body(Matchers.is("NEW SCRIPT"));

    }
}
