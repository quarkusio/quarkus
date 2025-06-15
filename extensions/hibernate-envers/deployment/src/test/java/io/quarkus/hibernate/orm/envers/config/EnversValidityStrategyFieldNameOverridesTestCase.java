package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversValidityStrategyFieldNameOverridesTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MyAuditedEntity.class, EnversTestValidityStrategyFieldNameOverridesResource.class,
                    AbstractEnversResource.class)
            .addAsResource("application-with-validity-strategy-field-name-overrides.properties",
                    "application.properties"));

    @Test
    public void testValidityStrategyFieldNameOverrides() {
        RestAssured.when().get("/envers-validity-strategy-field-name-overrides").then().body(is("OK"));
    }
}
