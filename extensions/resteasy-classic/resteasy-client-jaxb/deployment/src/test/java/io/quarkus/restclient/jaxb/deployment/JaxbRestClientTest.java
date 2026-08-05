package io.quarkus.restclient.jaxb.deployment;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class JaxbRestClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application.properties")
            .withApplicationRoot((jar) -> jar
                    .addClasses(Book.class,
                            HelloResource.class,
                            RestInterface.class,
                            ClientResource.class));

    @Test
    public void testJaxb() {
        RestAssured.get("/client/hello").then()
                .body(is("OK"));
    }
}
