package io.quarkus.hibernate.orm.quoted_indentifiers;

import static org.hamcrest.core.StringContains.containsString;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * Failed to fetch entity with reserved name.
 */
public class JPAQuotedTestCase {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Group.class, QuotedResource.class)
                    .addAsResource("application-quoted-identifiers.properties", "application.properties"));

    @Test
    public void testQuotedIdentifiers() {
        RestAssured.when().post("/jpa-test-quoted").then()
                .body(containsString("ok"));

        RestAssured.when().get("/jpa-test-quoted").then()
                .body(containsString("group_name"));
    }
}
