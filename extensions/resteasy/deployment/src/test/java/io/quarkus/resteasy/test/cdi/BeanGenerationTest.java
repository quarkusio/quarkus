package io.quarkus.resteasy.test.cdi;

import static io.restassured.RestAssured.when;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
