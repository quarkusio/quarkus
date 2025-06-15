package io.quarkus.info.deployment;

import static io.restassured.RestAssured.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DisabledInfoTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withEmptyApplication()
            .overrideConfigKey("quarkus.info.enabled", "false");

    @Test
    public void test() {
        when().get("/q/info").then().statusCode(404);

    }
}
