package io.quarkus.restclient.jackson.deployment;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class JacksonRestClientTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ZonedDateTimeObjectMapperCustomizer.class, DateDto.class, HelloResource.class,
                            RestInterface.class,
                            ClientResource.class));

    @Test
    public void testCustomDeserialization() {
        RestAssured.get("/client/hello").then()
                .body(is("OK"));
    }
}
