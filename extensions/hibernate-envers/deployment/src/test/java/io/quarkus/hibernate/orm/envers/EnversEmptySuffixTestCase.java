package io.quarkus.hibernate.orm.envers;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversEmptySuffixTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyAuditedEntity.class, EnversTestEmptyTableSuffixResource.class)
                    .addAsResource("application-emptysuffixtable.properties", "application.properties"));

    @Test
    public void testAuditTableSuffix() {
        RestAssured.when().get("/audit-table-empty-suffix").then()
                .body(is("OK"));
    }

}
