package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversAllowIdentifierReuseTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MyAuditedEntity.class, EnversTestAllowIdentifierReuseResource.class,
                    AbstractEnversResource.class)
            .addAsResource("application-with-allow-identifier-reuse.properties", "application.properties"));

    @Test
    public void testValidityStrategyFieldNameOverrides() {
        RestAssured.when().get("/envers-allow-identifier-reuse").then().body(is("OK"));
    }
}
