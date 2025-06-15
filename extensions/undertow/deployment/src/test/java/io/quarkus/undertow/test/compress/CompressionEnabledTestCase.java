package io.quarkus.undertow.test.compress;

import static io.restassured.RestAssured.get;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CompressionEnabledTestCase {

    @RegisterExtension
    static QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(SimpleServlet.class))
            .overrideConfigKey("quarkus.http.enable-compression", "true");

    @Test
    public void testCompressed() throws Exception {
        String bodyStr = get(SimpleServlet.SERVLET_ENDPOINT).then().statusCode(200).header("Content-Encoding", "gzip")
                .extract().asString();
        assertEquals("ok", bodyStr);
    }
}
