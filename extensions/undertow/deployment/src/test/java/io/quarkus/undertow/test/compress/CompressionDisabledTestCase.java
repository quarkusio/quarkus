package io.quarkus.undertow.test.compress;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

public class CompressionDisabledTestCase {

    @RegisterExtension
    static QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(SimpleServlet.class))
            .overrideConfigKey("quarkus.http.enable-compression", "false");

    @Test
    public void testCompressed() throws Exception {
        ExtractableResponse<Response> response = get(SimpleServlet.SERVLET_ENDPOINT).then().statusCode(200).extract();
        assertTrue(response.header("Content-Encoding") == null, response.headers().toString());
        assertEquals("ok", response.asString());
    }
}
