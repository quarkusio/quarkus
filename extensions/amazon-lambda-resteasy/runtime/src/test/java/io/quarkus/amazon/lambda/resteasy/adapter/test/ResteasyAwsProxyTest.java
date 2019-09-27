package io.quarkus.amazon.lambda.resteasy.adapter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
public class ResteasyAwsProxyTest {
    private static final String CUSTOM_HEADER_KEY = "x-custom-header";
    private static final String CUSTOM_HEADER_VALUE = "my-custom-value";
    private static final String AUTHORIZER_PRINCIPAL_ID = "test-principal-" + UUID.randomUUID().toString();
    private static final String USER_PRINCIPAL = "user1";

    private static ObjectMapper objectMapper = new ObjectMapper();

    private static ResteasyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    private static Context lambdaContext = new MockLambdaContext();

    @BeforeAll
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

    private AwsProxyRequestBuilder getRequestBuilder(boolean isAlb, String path, String method) {
        AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder(path, method);
        if (isAlb)
            builder.alb();

        return builder;
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void alb_basicRequest_expectSuccess(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/headers", "GET")
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

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void headers_getHeaders_echo(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/headers", "GET")
                .json()
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void headers_servletRequest_echo(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/servlet-headers", "GET")
                .json()
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void context_servletResponse_setCustomHeader(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/servlet-response", "GET")
                .json()
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertTrue(output.getMultiValueHeaders().containsKey(EchoResteasyResource.SERVLET_RESP_HEADER_KEY));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void context_serverInfo_correctContext(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/servlet-context", "GET").build();
        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        for (String header : output.getMultiValueHeaders().keySet()) {
            System.out.println(header + ": " + output.getMultiValueHeaders().getFirst(header));
        }
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, AwsServletContext.SERVER_INFO);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void requestScheme_valid_expectHttps(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/scheme", "GET")
                .json()
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, "https");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void requestFilter_injectsServletRequest_expectCustomAttribute(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/filter-attribute", "GET")
                .json()
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, ServletRequestFilter.FILTER_ATTRIBUTE_VALUE);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void authorizer_securityContext_customPrincipalSuccess(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/authorizer-principal", "GET")
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

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void authorizer_securityContext_customAuthorizerContextSuccess(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/authorizer-context", "GET")
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

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void errors_unknownRoute_expect404(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/test33", "GET").build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void error_contentType_invalidContentType(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/json-body", "POST")
                .header("Content-Type", "application/octet-stream")
                .body("asdasdasd")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(415, output.getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void error_statusCode_methodNotAllowed(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/status-code", "POST")
                .json()
                .queryString("status", "201")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(405, output.getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void responseBody_responseWriter_validBody(boolean isAlb) throws JsonProcessingException {
        SingleValueModel singleValueModel = new SingleValueModel();
        singleValueModel.setValue(CUSTOM_HEADER_VALUE);
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/json-body", "POST")
                .json()
                .body(objectMapper.writeValueAsString(singleValueModel))
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertNotNull(output.getBody());

        validateSingleValueModel(output, CUSTOM_HEADER_VALUE);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void statusCode_responseStatusCode_customStatusCode(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/status-code", "GET")
                .json()
                .queryString("status", "201")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(201, output.getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void base64_binaryResponse_base64Encoding(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/binary", "GET").build();

        AwsProxyResponse response = handler.proxy(request, lambdaContext);
        assertNotNull(response.getBody());
        assertTrue(Base64.isBase64(response.getBody()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void exception_mapException_mapToNotImplemented(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/exception", "GET").build();

        AwsProxyResponse response = handler.proxy(request, lambdaContext);
        assertNotNull(response.getBody());
        assertEquals(EchoResteasyResource.EXCEPTION_MESSAGE, response.getBody());
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void stripBasePath_route_shouldRouteCorrectly(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/custompath/echo/status-code", "GET")
                .json()
                .queryString("status", "201")
                .build();
        handler.stripBasePath("/custompath");
        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(201, output.getStatusCode());
        handler.stripBasePath("");
    }

    /**
     * In the case of RESTEasy, we properly route things as the AWS library includes the
     * stripBasePath element inside the contextPath so it's automatically removed
     * from the URL to resolve.
     * <p>
     * The name of this test is not really consistent with what it is doing now but
     * we keep it that way for consistency with the Jersey tests.
     */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void stripBasePath_route_shouldReturn404WithStageAsContext(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/custompath/echo/status-code", "GET")
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

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void stripBasePath_route_shouldReturn404(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/custompath/echo/status-code", "GET")
                .json()
                .queryString("status", "201")
                .build();
        handler.stripBasePath("/custom");
        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
        handler.stripBasePath("");
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void securityContext_injectPrincipal_expectPrincipalName(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/security-context", "GET")
                .authorizerPrincipal(USER_PRINCIPAL).build();

        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, USER_PRINCIPAL);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void emptyStream_putNullBody_expectPutToSucceed(boolean isAlb) {
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/empty-stream/" + CUSTOM_HEADER_KEY + "/test/2", "PUT")
                .nullBody()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .build();
        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, CUSTOM_HEADER_KEY);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void refererHeader_headerParam_expectCorrectInjection(boolean isAlb) {
        String refererValue = "test-referer";
        AwsProxyRequest request = getRequestBuilder(isAlb, "/echo/referer-header", "GET")
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
