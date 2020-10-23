package io.quarkus.funqy.test;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class SingleFunctionTest {
    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(SingleFunction.class));

    @Test
    public void testRoot() {
        // When single function is registered by @Funq, the function is mapped also to "/"
        RestAssured.given().contentType("application/json").body("\"Some string.\"").post("/")
                .then().statusCode(200).body(equalTo("\"Some string.\""));
    }
}
