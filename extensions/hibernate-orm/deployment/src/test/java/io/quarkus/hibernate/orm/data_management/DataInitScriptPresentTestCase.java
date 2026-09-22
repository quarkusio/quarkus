package io.quarkus.hibernate.orm.data_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class DataInitScriptPresentTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class)
                    .addAsResource("application-other-load-script-test.properties", "application.properties")
                    .addAsResource("load-script-test.sql"));

    @Test
    public void testDataInitScriptPresent() {
        String name = "other-load-script sql load script entity";
        RestAssured.when().get("/orm-init-script/3").then().body(Matchers.is(name));
    }
}
