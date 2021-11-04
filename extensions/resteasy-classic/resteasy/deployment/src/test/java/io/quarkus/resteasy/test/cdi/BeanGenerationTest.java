package io.quarkus.resteasy.test.cdi;

import static io.restassured.RestAssured.when;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.test.cdi.internal.PublicHello;
import io.quarkus.test.QuarkusUnitTest;

/**
 *
 */
public class BeanGenerationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(PublicHello.class.getPackage())
                    .addClasses(Greeting.class, MorningGreeting.class, GreetingEndpoint.class));

    @Test
    public void testInvocation() throws Exception {
        when().get("/cdi-greeting/greet")
                .then()
                .statusCode(200)
                .body(Matchers.is("Good Morning"));
    }
}
