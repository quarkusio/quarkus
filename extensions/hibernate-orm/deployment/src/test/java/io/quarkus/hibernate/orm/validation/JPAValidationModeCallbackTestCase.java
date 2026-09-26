package io.quarkus.hibernate.orm.validation;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class JPAValidationModeCallbackTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, JPATestValidationResource.class))
            .overrideConfigKey("quarkus.hibernate-orm.validation.mode", "callback");

    @Test
    public void testInvalidEntity() {
        RestAssured.given().body(
                "The POST method should not persist an entity whose name exceeds the maximum allowed length.")
                .when().post("/validation").then()
                .body(is(MyEntity.ENTITY_NAME_TOO_LONG));
    }

    @Test
    public void testDDL() {
        RestAssured.when().get("/validation").then()
                .body(is("nullable: true"));
    }
}
