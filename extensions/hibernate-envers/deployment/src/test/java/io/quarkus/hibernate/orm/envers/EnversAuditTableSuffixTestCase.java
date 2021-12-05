package io.quarkus.hibernate.orm.envers;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversAuditTableSuffixTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyAuditedEntity.class, MyRevisionEntity.class, MyRevisionListener.class,
                            EnversTestAuditTableSuffixResource.class)
                    .addAsResource("application-with-store-data-at-delete.properties", "application.properties"));

    @Test
    public void testAuditTableSuffix() {
        RestAssured.when().get("/audit-table-suffix").then()
                .body(is("OK"));
    }

}
