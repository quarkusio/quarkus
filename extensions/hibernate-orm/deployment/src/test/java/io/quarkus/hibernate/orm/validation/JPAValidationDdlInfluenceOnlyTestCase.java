package io.quarkus.hibernate.orm.validation;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Tests that DDL influence can be enabled independently of lifecycle validation callbacks
 * using the new {@code quarkus.hibernate-orm.validation.ddl-influence} property.
 */
public class JPAValidationDdlInfluenceOnlyTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, JPATestValidationResource.class))
            .overrideConfigKey("quarkus.hibernate-orm.validation.mode", "none")
            .overrideConfigKey("quarkus.hibernate-orm.validation.ddl-influence", "required");

    @Test
    public void testCallbackValidationDisabled() {
        // We use an empty name here because @NotEmpty is a callback-only constraint with no DDL equivalent.
        // A too-long name would be rejected by the database itself (DDL influence creates a VARCHAR(50) column),
        // so it cannot be used to prove that callback validation is off.
        RestAssured.given().body("").when().post("/validation").then()
                .body(is("OK"));
    }

    @Test
    public void testDdlInfluenceEnabled() {
        RestAssured.when().get("/validation").then()
                .body(is("nullable: false"));
    }
}
