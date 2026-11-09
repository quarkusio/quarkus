package io.quarkus.vertx.http.crac;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;
import io.restassured.RestAssured;

public class CracDisabledProdModeTest {

    @RegisterExtension
    static final QuarkusProdModeTest TEST = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar.addClass(CracRoute.class))
            .setRun(true);

    @Test
    public void testCracSupport() {
        RestAssured.get("/crac").then().statusCode(200).body(is("false"));
    }
}
