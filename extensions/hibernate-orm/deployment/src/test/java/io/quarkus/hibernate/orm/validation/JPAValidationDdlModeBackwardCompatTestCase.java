package io.quarkus.hibernate.orm.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Tests backward compatibility when only the deprecated 'ddl' validation mode is used
 * without an explicit {@code ddl-influence} value. The 'ddl' value is translated to
 * {@code ddl-influence=required} and validation mode becomes 'none'.
 */
public class JPAValidationDdlModeBackwardCompatTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyEntity.class, JPATestValidationResource.class))
            .overrideConfigKey("quarkus.hibernate-orm.validation.mode", "ddl");

    @Inject
    EntityManagerFactory emf;

    @Test
    public void testLegacyDdlRequiresValidationProvider() {
        assertThat(emf.getProperties())
                .containsEntry(AvailableSettings.APPLY_VALIDATION_CONSTRAINTS, "required");
    }

    @Test
    public void testCallbackValidationDisabled() {
        RestAssured.given().body("").when().post("/validation").then()
                .body(is("OK"));
    }

    @Test
    public void testDdlInfluenceEnabled() {
        RestAssured.when().get("/validation").then()
                .body(is("nullable: false"));
    }
}
