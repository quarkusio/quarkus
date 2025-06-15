package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversModifiedColumnNamingStrategyTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyAuditedEntity.class, EnversTestModifiedColumnNamingStrategyResource.class,
                            AbstractEnversResource.class)
                    .addAsResource("application-with-modified-column-naming-strategy.properties",
                            "application.properties"));

    @Test
    public void testModifiedColumnNamingStrategy() {
        RestAssured.when().get("/envers-modified-column-naming-strategy").then().body(is("OK"));
    }
}
