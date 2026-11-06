package io.quarkus.hibernate.orm.validation;

import static org.hamcrest.Matchers.is;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class JPATestDdlAutoNoValidator {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-hibernate-validator"),
                    ArtifactKey.of("io.quarkus", "quarkus-hibernate-validator-deployment"),
                    ArtifactKey.of("org.hibernate.validator", "hibernate-validator")))
            .overrideConfigKey("quarkus.hibernate-orm.validation.mode", "none")
            .overrideConfigKey("quarkus.hibernate-orm.validation.ddl-influence", "required")
            .setExpectedException(RuntimeException.class);

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
