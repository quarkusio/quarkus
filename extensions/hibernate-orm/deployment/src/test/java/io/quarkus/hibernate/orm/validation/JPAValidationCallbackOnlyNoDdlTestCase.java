package io.quarkus.hibernate.orm.validation;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Tests that lifecycle validation callbacks can be enabled while DDL influence is independently disabled
 * using the new {@code quarkus.hibernate-orm.validation.ddl-influence} property.
 */
public class JPAValidationCallbackOnlyNoDdlTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, JPATestValidationResource.class))
            .overrideConfigKey("quarkus.hibernate-orm.validation.mode", "callback")
            .overrideConfigKey("quarkus.hibernate-orm.validation.ddl-influence", "disabled");

    @Test
    public void testCallbackValidationEnabled() {
        RestAssured.given().body(
                "The POST method should not persist an entity whose name exceeds the maximum allowed length.")
                .when().post("/validation").then()
                .body(is(MyEntity.ENTITY_NAME_TOO_LONG));
    }

    @Test
    public void testDdlInfluenceDisabled() {
        RestAssured.when().get("/validation").then()
                .body(is("nullable: true"));
    }
}
