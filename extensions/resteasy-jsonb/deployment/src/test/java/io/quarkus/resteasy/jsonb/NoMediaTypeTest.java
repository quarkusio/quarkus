package io.quarkus.resteasy.jsonb;

import static org.hamcrest.Matchers.equalTo;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class NoMediaTypeTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(HelloNoMediaTypeResource.class, Message.class));

    @Test
    public void testJsonDefaultNoProduces() {
        RestAssured.get("/hello-default").then()
                .statusCode(200)
                .body("message", equalTo("Hello"));
    }

}
