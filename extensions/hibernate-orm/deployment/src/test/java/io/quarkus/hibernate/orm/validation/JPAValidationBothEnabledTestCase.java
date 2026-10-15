package io.quarkus.hibernate.orm.validation;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Tests that both lifecycle validation callbacks and DDL influence work when independently enabled
 * using the new {@code quarkus.hibernate-orm.validation.ddl-influence} property.
 */
public class JPAValidationBothEnabledTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, JPATestValidationResource.class))
            .overrideConfigKey("quarkus.hibernate-orm.validation.mode", "callback")
            .overrideConfigKey("quarkus.hibernate-orm.validation.ddl-influence", "auto");

    @Test
    public void testCallbackValidationEnabled() {
        RestAssured.given().body(
                "The POST method should not persist an entity whose name exceeds the maximum allowed length.")
                .when().post("/validation").then()
                .body(is(MyEntity.ENTITY_NAME_TOO_LONG));
    }

    @Test
    public void testDdlInfluenceEnabled() {
        RestAssured.when().get("/validation").then()
                .body(is("nullable: false"));
    }
}
