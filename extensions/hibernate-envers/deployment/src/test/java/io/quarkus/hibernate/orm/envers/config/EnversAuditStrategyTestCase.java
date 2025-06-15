package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversAuditStrategyTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MyAuditedEntity.class, EnversTestAuditStrategyResource.class, AbstractEnversResource.class)
            .addAsResource("application-with-audit-strategy.properties", "application.properties"));

    @Test
    public void testAuditStrategy() {
        RestAssured.when().get("/envers-audit-strategy").then().body(is("OK"));
    }
}
