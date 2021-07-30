package io.quarkus.funqy.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class WithConflictingAttributeFilterTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().assertException(t -> {
        Throwable i = t;
        boolean found = false;
        while (i != null) {
            if (i instanceof IllegalStateException) {
                found = true;
                break;
            }
            i = i.getCause();
        }

        assertTrue(found, "Build failed with wrong exception, expected IllegalStateException but got " + t);
    })
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(WithConflictingAttributeFilter.class, Identity.class));

    @Test
    public void testAttributeFilterMatch() {
        RestAssured.given().contentType("application/json")
                .body("[{\"name\": \"Bill\"}, {\"name\": \"Matej\"}]")
                .header("ce-id", "42")
                .header("ce-type", "listOfStrings")
                .header("ce-source", "test")
                .header("ce-specversion", "1.0")
                .post("/")
                .then().statusCode(404);
    }

}
