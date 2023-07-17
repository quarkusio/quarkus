package io.quarkus.grpc.server.devmode;

import static io.restassured.RestAssured.when;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.response.Response;

public class DevModeServerStartDisabledTest {
    @RegisterExtension
    public static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IsUpEndpoint.class)
                    .add(new StringAsset("quarkus.grpc.dev-mode.force-server-start=false\n"), "application.properties"));

    @Test
    public void test() {
        Response response = when().get("/grpc-status");
        response.then().statusCode(204); // 200 is returned when server started, 204 otherwise
    }
}
