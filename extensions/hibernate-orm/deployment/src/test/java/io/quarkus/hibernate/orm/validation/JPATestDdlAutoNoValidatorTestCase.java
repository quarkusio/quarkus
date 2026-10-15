package io.quarkus.hibernate.orm.validation;

import static org.hamcrest.Matchers.is;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class JPATestDdlAutoNoValidatorTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, JPATestValidationResource.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-hibernate-validator"),
                    ArtifactKey.of("io.quarkus", "quarkus-hibernate-validator-deployment"),
                    ArtifactKey.of("org.hibernate.validator", "hibernate-validator")))
            .overrideConfigKey("quarkus.hibernate-orm.validation.mode", "none")
            .overrideConfigKey("quarkus.hibernate-orm.validation.ddl-influence", "auto");

    @Test
    public void testStartupSucceedsWithoutDdlInfluence() {
        // MyEntity.name has @NotNull, but without the Validator extension AUTO must skip DDL influence.
        RestAssured.when().get("/validation").then()
                .statusCode(200)
                .body(is("nullable: true"));
    }
}
