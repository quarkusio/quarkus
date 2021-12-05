package io.quarkus.grpc.server.devmode;

import static io.restassured.RestAssured.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.response.Response;

public class DevModeServerStartTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IsUpEndpoint.class));

    @Test
    public void test() {
        Response response = when().get("/grpc-status");
        response.then().statusCode(200); // 200 is returned when server started, 204 otherwise
    }
}
