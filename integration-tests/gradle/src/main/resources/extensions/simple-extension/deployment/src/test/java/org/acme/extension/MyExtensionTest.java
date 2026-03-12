package org.acme.extension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.Matchers.containsString;

public class MyExtensionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyExtensionProcessor.class));

    @Test
    public void testGreeting() {
        RestAssured.when().get("/my-extension").then().statusCode(200).body(containsString("Hello"));
    }
}
