package io.quarkus.vertx.http.crac;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

public class CracEnabledProdModeTest {

    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClass(CracRoute.class))
            .overrideConfigKey("quarkus.package.jar.crac.enabled", "true")
            .setRun(true);

    @Test
    public void testCracSupport() {
        RestAssured.get("/crac").then().statusCode(200).body(is("true"));
    }
}
