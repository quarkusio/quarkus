package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedVersionEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversDoNotAuditOptimisticLockingFieldTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MyAuditedVersionEntity.class, EnversTestDoNotAuditOptimisticLockingFieldResource.class,
                    AbstractEnversResource.class)
            .addAsResource("application-with-do-not-audit-optimistic-locking-field.properties",
                    "application.properties"));

    @Test
    public void testDoNotAuditOptimisticLockingFieldAsNonDefault() {
        RestAssured.when().get("/envers-do-not-audit-optimistic-locking-field").then().body(is("OK"));
    }
}
