package io.quarkus.resteasy.jsonb;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class NoMediaTypeTest {

    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloNoMediaTypeResource.class, Message.class));

    @Test
    public void testJsonDefaultNoProduces() {
        RestAssured.get("/hello-default").then()
                .statusCode(200)
                .body("message", equalTo("Hello"));
    }

}
