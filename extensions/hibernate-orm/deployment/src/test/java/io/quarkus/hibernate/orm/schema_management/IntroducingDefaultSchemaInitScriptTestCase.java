package io.quarkus.hibernate.orm.schema_management;

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
public class IntroducingDefaultSchemaInitScriptTestCase {

    @RegisterExtension
    static QuarkusDevModeTest runner = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties")
                    .addClasses(InitScriptTestResource.class, MyEntity.class));

    @Test
    public void testIntroducingDefaultSchemaInitScript() {
        RestAssured.when().get("/orm-init-script/1").then().body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));

        runner.addResourceFile("import.sql", "INSERT INTO MyEntity(id, name) VALUES(1, 'NEW SCRIPT');");

        RestAssured.when().get("/orm-init-script/1").then().body(Matchers.is("NEW SCRIPT"));
    }

}
