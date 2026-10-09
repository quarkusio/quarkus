package io.quarkus.hibernate.orm.validation;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Tests that both lifecycle validation callbacks and DDL influence are disabled when
 * independently configured using the new {@code quarkus.hibernate-orm.validation.ddl-influence} property.
 */
public class JPAValidationBothDisabledTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, JPATestValidationResource.class))
            .overrideConfigKey("quarkus.hibernate-orm.validation.mode", "none")
            .overrideConfigKey("quarkus.hibernate-orm.validation.ddl-influence", "disabled");

    @Test
    public void testCallbackValidationDisabled() {
        String entityName = "Post method should succeed because callback validation is disabled even though name exceeds the 50-character limit.";
        RestAssured.given().body(entityName).when().post("/validation").then()
                .body(is("OK"));
    }

    @Test
    public void testDdlInfluenceDisabled() {
        RestAssured.when().get("/validation").then()
                .body(is("nullable: true"));
    }
}
