package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class EnversModifiedFlagsTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyAuditedEntity.class, EnversTestModifiedFlagsResource.class,
                            AbstractEnversResource.class)
                    .addAsResource("application-with-modified-flags.properties",
                            "application.properties"));

    @Test
    public void testModifiedFlags() {
        RestAssured.when().get("/envers-modified-flags").then()
                .body(is("OK"));
    }
}
