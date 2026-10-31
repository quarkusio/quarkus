package io.quarkus.spring.web.resteasy.reactive.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class InterfaceControllerTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(GreetingControllerInterface.class, GreetingControllerImpl.class,
                            ConcreteGreetingController.class));

    @Test
    public void testInterfaceController() {
        when().get("/interface-greeting")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("hello from interface"));
    }

    @Test
    public void testConcreteController() {
        when().get("/concrete-greeting")
                .then()
                .statusCode(200)
                .contentType("text/plain")
                .body(is("hello from concrete"));
    }
}
