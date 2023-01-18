package io.quarkus.it.virtual;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;

import io.quarkus.azure.functions.resteasy.runtime.Function;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Unit test for Function class.
 */
@QuarkusTest
public class FunctionTest {

    @Test
    public void testFunqy() {
        final HttpRequestMessageMock req = new HttpRequestMessageMock();
        req.setUri(URI.create("https://foo.com/funqy"));
        req.setHttpMethod(HttpMethod.POST);
        req.setBody("\"Bill\"");
        req.getHeaders().put("Content-Type", "application/json");

        // Invoke
        final HttpResponseMessage ret = new Function().run(req, new ExecutionContext() {
            @Override
            public Logger getLogger() {
                return null;
            }

            @Override
            public String getInvocationId() {
                return null;
            }

            @Override
            public String getFunctionName() {
                return null;
            }
        });

        // Verify
        Assertions.assertEquals(ret.getStatus(), HttpStatus.OK);
        Assertions.assertEquals("\"Make it funqy Bill\"", new String((byte[]) ret.getBody(), StandardCharsets.UTF_8));
        String contentType = ret.getHeader("Content-Type");
        Assertions.assertNotNull(contentType);
        Assertions.assertTrue(MediaType.valueOf(contentType).isCompatible(MediaType.APPLICATION_JSON_TYPE));
    }

    @Test
    public void testJaxrs() throws Exception {
        String uri = "https://foo.com/hello";
        testGET(uri);
        testPOST(uri);
    }

    @Test
    public void testServlet() throws Exception {
        String uri = "https://foo.com/servlet/hello";
        testGET(uri);
        testPOST(uri);
    }

    @Test
    public void testVertx() throws Exception {
        String uri = "https://foo.com/vertx/hello";
        testGET(uri);
        testPOST(uri);
        uri = "https://foo.com/vertx/exchange/hello";
        testGET(uri);
    }

    @Test
    public void testNotFound() {
        final HttpRequestMessageMock req = new HttpRequestMessageMock();
        req.setUri(URI.create("https://nowhere.com/badroute"));
        req.setHttpMethod(HttpMethod.GET);

        // Invoke
        final HttpResponseMessage ret = new Function().run(req, new ExecutionContext() {
            @Override
            public Logger getLogger() {
                return null;
            }

            @Override
            public String getInvocationId() {
                return null;
            }

            @Override
            public String getFunctionName() {
                return null;
            }
        });

        // Verify
        Assertions.assertEquals(ret.getStatus(), HttpStatus.NOT_FOUND);
    }

    @Test
    public void testHttp() {
        // assure that socket is created in dev/test mode
        RestAssured.when().get("/hello").then()
                .contentType("text/plain")
                .body(equalTo("hello"));

        RestAssured.given().contentType("text/plain").body("Bill").post("/hello").then()
                .contentType("text/plain")
                .body(containsString("hello Bill"));
    }

    private void testGET(String uri) {
        final HttpRequestMessageMock req = new HttpRequestMessageMock();
        req.setUri(URI.create(uri));
        req.setHttpMethod(HttpMethod.GET);

        // Invoke
        final HttpResponseMessage ret = new Function().run(req, new ExecutionContext() {
            @Override
            public Logger getLogger() {
                return null;
            }

            @Override
            public String getInvocationId() {
                return null;
            }

            @Override
            public String getFunctionName() {
                return null;
            }
        });

        // Verify
        Assertions.assertEquals(ret.getStatus(), HttpStatus.OK);
        Assertions.assertEquals("hello", new String((byte[]) ret.getBody(), StandardCharsets.UTF_8));
        String contentType = ret.getHeader("Content-Type");
        Assertions.assertNotNull(contentType);
        Assertions.assertTrue(MediaType.valueOf(contentType).isCompatible(MediaType.TEXT_PLAIN_TYPE));
    }

    private void testPOST(String uri) {
        final HttpRequestMessageMock req = new HttpRequestMessageMock();
        req.setUri(URI.create(uri));
        req.setHttpMethod(HttpMethod.POST);
        req.setBody("Bill");
        req.getHeaders().put("Content-Type", "text/plain");

        // Invoke
        final HttpResponseMessage ret = new Function().run(req, new ExecutionContext() {
            @Override
            public Logger getLogger() {
                return null;
            }

            @Override
            public String getInvocationId() {
                return null;
            }

            @Override
            public String getFunctionName() {
                return null;
            }
        });

        // Verify
        Assertions.assertEquals(ret.getStatus(), HttpStatus.OK);
        Assertions.assertEquals("hello Bill", new String((byte[]) ret.getBody(), StandardCharsets.UTF_8));
        String contentType = ret.getHeader("Content-Type");
        Assertions.assertNotNull(contentType);
        Assertions.assertTrue(MediaType.valueOf(contentType).isCompatible(MediaType.TEXT_PLAIN_TYPE));
    }

}
