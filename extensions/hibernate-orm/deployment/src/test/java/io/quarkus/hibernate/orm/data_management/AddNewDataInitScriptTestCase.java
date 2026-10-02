package io.quarkus.hibernate.orm.data_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.TestTags;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Tag(TestTags.DEVMODE)
public class AddNewDataInitScriptTestCase {
    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addAsResource("data.sql")
                    .addClasses(InitScriptTestResource.class, MyEntity.class));

    @Test
    public void testAddNewDataInitScript() {
        String name = "data.sql data init script entity";
        RestAssured.when().get("/orm-init-script/10").then().body(Matchers.is(name));

        runner.modifyResourceFile("application.properties",
                (s) -> s += "\nquarkus.hibernate-orm.data-management.init-script=new-data-init-script.sql");
        runner.addResourceFile("new-data-init-script.sql", "INSERT INTO MyEntity(id, name) VALUES(10, 'NEW SCRIPT');");
        RestAssured.when().get("/orm-init-script/10").then().body(Matchers.is("NEW SCRIPT"));

    }
}
