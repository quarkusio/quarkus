package io.quarkus.swaggerui.deployment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ManagementInterfaceTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.management.enabled", "true");

    @Test
    public void shouldNotSetServerForManagementInterface() {
        RestAssured.when().get("http://localhost:9001/q/swagger-ui/index.html").then().statusCode(200)
                .body(not(containsString("function RelativeServerPlugin()")));
    }
}
