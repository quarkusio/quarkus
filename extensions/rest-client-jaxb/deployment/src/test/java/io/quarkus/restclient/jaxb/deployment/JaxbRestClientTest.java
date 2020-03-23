package io.quarkus.restclient.jaxb.deployment;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class JaxbRestClientTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
