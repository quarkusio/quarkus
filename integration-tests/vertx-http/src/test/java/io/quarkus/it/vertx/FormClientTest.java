package io.quarkus.it.vertx;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FormClientTest {

    @Test
    public void testSendForm() {
        get("/form-client").then().statusCode(200).body(is("Hello Quarkus"));
    }
}
