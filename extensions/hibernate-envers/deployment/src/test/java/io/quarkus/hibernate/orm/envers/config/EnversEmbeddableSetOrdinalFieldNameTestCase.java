package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversEmbeddableSetOrdinalFieldNameTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyAuditedEntity.class, EnversTestEmbeddableSetOrdinalFieldNameResource.class,
                            AbstractEnversResource.class)
                    .addAsResource("application-with-embeddable-set-ordinal-field-name.properties",
                            "application.properties"));

    @Test
    public void testDoNotAuditOptimisticLockingFieldAsNonDefault() {
        RestAssured.when().get("/envers-embeddable-set-ordinal-field-name").then().body(is("OK"));
    }
}
