package io.quarkus.resteasy.jackson;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class NoMediaTypeTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloNoMediaTypeResource.class, Message.class));

    @Test
    public void testJsonDefaultNoProduces() {
        RestAssured.get("/hello-default").then()
                .statusCode(200)
                .body("message", equalTo("Hello"));
    }

}
