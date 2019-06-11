package io.quarkus.amazon.lambda.resteasy.adapter.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.AwsServletContext;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.amazon.lambda.resteasy.adapter.test.model.MapResponseModel;
import io.quarkus.amazon.lambda.resteasy.adapter.test.model.SingleValueModel;
import io.quarkus.amazon.lambda.resteasy.adapter.test.provider.CustomExceptionMapper;
import io.quarkus.amazon.lambda.resteasy.adapter.test.provider.ServletRequestFilter;
import io.quarkus.amazon.lambda.resteasy.runtime.container.ResteasyLambdaContainerHandler;

/**
 * Unit test class for the RESTEasy AWS_PROXY default implementation
 */
@RunWith(Parameterized.class)
public class ResteasyAwsProxyTest {
    private static final String CUSTOM_HEADER_KEY = "x-custom-header";
    private static final String CUSTOM_HEADER_VALUE = "my-custom-value";
    private static final String AUTHORIZER_PRINCIPAL_ID = "test-principal-" + UUID.randomUUID().toString();
    private static final String USER_PRINCIPAL = "user1";

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static ResteasyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    private static Context lambdaContext = new MockLambdaContext();

    private boolean isAlb;

    public ResteasyAwsProxyTest(boolean alb) {
        isAlb = alb;
    }

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] { false, true });
    }

    @BeforeClass
    public static void setup() {
        Map<String, String> initParameters = new HashMap<>();
        initParameters.put(ResteasyContextParameters.RESTEASY_SCANNED_RESOURCES, EchoResteasyResource.class.getName());
        initParameters.put("resteasy.servlet.mapping.prefix", "/");
        initParameters.put(ResteasyContextParameters.RESTEASY_USE_BUILTIN_PROVIDERS, "true");
        initParameters.put(ResteasyContextParameters.RESTEASY_PROVIDERS, new StringJoiner(",")
                .add(CustomExceptionMapper.class.getName())
                .add(ServletRequestFilter.class.getName()).toString());

        handler = ResteasyLambdaContainerHandler.getAwsProxyHandler(initParameters);
    }

    private AwsProxyRequestBuilder getRequestBuilder(String path, String method) {
        AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder(path, method);
        if (isAlb)
            builder.alb();

        return builder;
    }

    @Test
    public void alb_basicRequest_expectSuccess() {
        AwsProxyRequest request = getRequestBuilder("/echo/headers", "GET")
                .json()
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .alb()
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));
        assertNotNull(output.getStatusDescription());
        System.out.println(output.getStatusDescription());

        validateMapResponseModel(output);
    }

    @Test
    public void headers_getHeaders_echo() {
        AwsProxyRequest request = getRequestBuilder("/echo/headers", "GET")
                .json()
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output);
    }

    @Test
    public void headers_servletRequest_echo() {
        AwsProxyRequest request = getRequestBuilder("/echo/servlet-headers", "GET")
                .json()
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output);
    }

    @Test
    public void context_servletResponse_setCustomHeader() {
        AwsProxyRequest request = getRequestBuilder("/echo/servlet-response", "GET")
                .json()
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertTrue(output.getMultiValueHeaders().containsKey(EchoResteasyResource.SERVLET_RESP_HEADER_KEY));
    }

    @Test
    public void context_serverInfo_correctContext() {
        AwsProxyRequest request = getRequestBuilder("/echo/servlet-context", "GET").build();
        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        for (String header : output.getMultiValueHeaders().keySet()) {
            System.out.println(header + ": " + output.getMultiValueHeaders().getFirst(header));
        }
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, AwsServletContext.SERVER_INFO);
    }

    @Test
    public void requestScheme_valid_expectHttps() {
        AwsProxyRequest request = getRequestBuilder("/echo/scheme", "GET")
                .json()
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, "https");
    }

    @Test
    public void requestFilter_injectsServletRequest_expectCustomAttribute() {
        AwsProxyRequest request = getRequestBuilder("/echo/filter-attribute", "GET")
                .json()
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, ServletRequestFilter.FILTER_ATTRIBUTE_VALUE);
    }

    @Test
    public void authorizer_securityContext_customPrincipalSuccess() {
        AwsProxyRequest request = getRequestBuilder("/echo/authorizer-principal", "GET")
                .json()
                .authorizerPrincipal(AUTHORIZER_PRINCIPAL_ID)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        if (!isAlb) {
            assertEquals(200, output.getStatusCode());
            assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));
            validateSingleValueModel(output, AUTHORIZER_PRINCIPAL_ID);
        }

    }

    @Test
    public void authorizer_securityContext_customAuthorizerContextSuccess() {
        AwsProxyRequest request = getRequestBuilder("/echo/authorizer-context", "GET")
                .json()
                .authorizerPrincipal(AUTHORIZER_PRINCIPAL_ID)
                .authorizerContextValue(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .queryString("key", CUSTOM_HEADER_KEY)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, CUSTOM_HEADER_VALUE);
    }

    @Test
    public void errors_unknownRoute_expect404() {
        AwsProxyRequest request = getRequestBuilder("/echo/test33", "GET").build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
    }

    @Test
    public void error_contentType_invalidContentType() {
        AwsProxyRequest request = getRequestBuilder("/echo/json-body", "POST")
                .header("Content-Type", "application/octet-stream")
                .body("asdasdasd")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(415, output.getStatusCode());
    }

    @Test
    public void error_statusCode_methodNotAllowed() {
        AwsProxyRequest request = getRequestBuilder("/echo/status-code", "POST")
                .json()
                .queryString("status", "201")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(405, output.getStatusCode());
    }

    @Test
    public void responseBody_responseWriter_validBody() throws JsonProcessingException {
        SingleValueModel singleValueModel = new SingleValueModel();
        singleValueModel.setValue(CUSTOM_HEADER_VALUE);
        AwsProxyRequest request = getRequestBuilder("/echo/json-body", "POST")
                .json()
                .body(objectMapper.writeValueAsString(singleValueModel))
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertNotNull(output.getBody());

        validateSingleValueModel(output, CUSTOM_HEADER_VALUE);
    }

    @Test
    public void statusCode_responseStatusCode_customStatusCode() {
        AwsProxyRequest request = getRequestBuilder("/echo/status-code", "GET")
                .json()
                .queryString("status", "201")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(201, output.getStatusCode());
    }

    @Test
    public void base64_binaryResponse_base64Encoding() {
        AwsProxyRequest request = getRequestBuilder("/echo/binary", "GET").build();

        AwsProxyResponse response = handler.proxy(request, lambdaContext);
        assertNotNull(response.getBody());
        assertTrue(Base64.isBase64(response.getBody()));
    }

    @Test
    public void exception_mapException_mapToNotImplemented() {
        AwsProxyRequest request = getRequestBuilder("/echo/exception", "GET").build();

        AwsProxyResponse response = handler.proxy(request, lambdaContext);
        assertNotNull(response.getBody());
        assertEquals(EchoResteasyResource.EXCEPTION_MESSAGE, response.getBody());
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void stripBasePath_route_shouldRouteCorrectly() {
        AwsProxyRequest request = getRequestBuilder("/custompath/echo/status-code", "GET")
                .json()
                .queryString("status", "201")
                .build();
        handler.stripBasePath("/custompath");
        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(201, output.getStatusCode());
        handler.stripBasePath("");
    }

    @Test
    /**
     * In the case of RESTEasy, we properly route things as the AWS library includes the
     * stripBasePath element inside the contextPath so it's automatically removed
     * from the URL to resolve.
     * <p>
     * The name of this test is not really consistent with what it is doing now but
     * we keep it that way for consistency with the Jersey tests.
     */
    public void stripBasePath_route_shouldReturn404WithStageAsContext() {
        AwsProxyRequest request = getRequestBuilder("/custompath/echo/status-code", "GET")
                .stage("prod")
                .json()
                .queryString("status", "201")
                .build();
        handler.stripBasePath("/custompath");
        LambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(true);
        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(201, output.getStatusCode());
        handler.stripBasePath("");
        LambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(false);
    }

    @Test
    public void stripBasePath_route_shouldReturn404() {
        AwsProxyRequest request = getRequestBuilder("/custompath/echo/status-code", "GET")
                .json()
                .queryString("status", "201")
                .build();
        handler.stripBasePath("/custom");
        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
        handler.stripBasePath("");
    }

    @Test
    public void securityContext_injectPrincipal_expectPrincipalName() {
        AwsProxyRequest request = getRequestBuilder("/echo/security-context", "GET")
                .authorizerPrincipal(USER_PRINCIPAL).build();

        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, USER_PRINCIPAL);
    }

    @Test
    public void emptyStream_putNullBody_expectPutToSucceed() {
        AwsProxyRequest request = getRequestBuilder("/echo/empty-stream/" + CUSTOM_HEADER_KEY + "/test/2", "PUT")
                .nullBody()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .build();
        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, CUSTOM_HEADER_KEY);
    }

    @Test
    public void refererHeader_headerParam_expectCorrectInjection() {
        String refererValue = "test-referer";
        AwsProxyRequest request = getRequestBuilder("/echo/referer-header", "GET")
                .nullBody()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .header("Referer", refererValue)
                .build();

        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, refererValue);
    }

    private void validateMapResponseModel(AwsProxyResponse output) {
        validateMapResponseModel(output, CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);
    }

    private void validateMapResponseModel(AwsProxyResponse output, String key, String value) {
        try {
            MapResponseModel response = objectMapper.readValue(output.getBody(), MapResponseModel.class);
            assertNotNull(response.getValues().get(key));
            assertEquals(value, response.getValues().get(key));
        } catch (IOException e) {
            fail("Exception while parsing response body: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateSingleValueModel(AwsProxyResponse output, String value) {
        try {
            SingleValueModel response = objectMapper.readValue(output.getBody(), SingleValueModel.class);
            assertNotNull(response.getValue());
            assertEquals(value, response.getValue());
        } catch (IOException e) {
            fail("Exception while parsing response body: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
