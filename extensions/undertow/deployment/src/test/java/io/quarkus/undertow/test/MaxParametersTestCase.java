package io.quarkus.undertow.test;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Tests the quarkus.servlet.max-parameters setting.
 */
public class MaxParametersTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestServlet.class, TestGreeter.class)
                    .addAsResource(new StringAsset("quarkus.servlet.max-parameters=10"), "application.properties"));

    @Test
    public void testSmallRequest() {
        RestAssured.given()
                .params(generateParameters(10))
                .get("/test")
                .then().statusCode(200).body(Matchers.equalTo("test servlet"));
    }

    @Test
    public void testLargeRequest() {
        // A throw of io.undertow.util.ParameterLimitException causes Undertow to close the
        // connection immediately without responding to the HTTP request.
        Assertions.assertThrows(SocketTimeoutException.class, () -> RestAssured.given()
                .params(generateParameters(11))
                .get("/test")
                .then().statusCode(414));
    }

    private static Map<String, String> generateParameters(int count) {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < count; i++) {
            params.put(String.format("q%d", i), String.format("%d", i));
        }
        return params;
    }
}
