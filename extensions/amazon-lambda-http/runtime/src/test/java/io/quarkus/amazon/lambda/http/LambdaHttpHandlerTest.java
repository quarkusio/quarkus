package io.quarkus.amazon.lambda.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaContext;
import io.quarkus.netty.runtime.virtual.VirtualAddress;
import io.quarkus.netty.runtime.virtual.VirtualChannel;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.netty.runtime.virtual.VirtualResponseHandler;
import io.quarkus.runtime.Application;

public class LambdaHttpHandlerTest {

    private static final long PROCESSING_TIMEOUT = TimeUnit.SECONDS.toMillis(1);

    private static final String PATH = "/test/path";
    private static final String QUERY = "testParam1=testValue1&testParam2=testValue2";
    private static final String HOST_HEADER = "Host";
    private static final String HOST = "localhost";

    private static final List<String> COOKIES = List.of("testcookie1=cvalue1", "testcookie2=cvalue2");
    private static final String COOKIE_HEADER_KEY = "Cookie";
    private static final String COOKIE_HEADER_VALUE = "testcookie1=cvalue1; testcookie2=cvalue2";

    private static final String METHOD = "GET";

    private final Application application = mock(Application.class);
    private final APIGatewayV2HTTPEvent request = mock(APIGatewayV2HTTPEvent.class);
    private final APIGatewayV2HTTPEvent.RequestContext requestContext = mock(APIGatewayV2HTTPEvent.RequestContext.class);
    private final APIGatewayV2HTTPEvent.RequestContext.Http requestContextMethod = mock(
            APIGatewayV2HTTPEvent.RequestContext.Http.class);
    private final AmazonLambdaContext context = mock(AmazonLambdaContext.class);
    private final VirtualClientConnection<?> connection = mock(VirtualClientConnection.class);
    private final VirtualChannel peer = mock(VirtualChannel.class);

    @BeforeEach
    public void mockSetup() {
        when(request.getRawPath()).thenReturn(PATH);
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getHttp()).thenReturn(requestContextMethod);
        when(requestContextMethod.getMethod()).thenReturn(METHOD);
        when(request.getHeaders()).thenReturn(Collections.singletonMap(HOST_HEADER, HOST));
        when(request.getCookies()).thenReturn(COOKIES);
        when(connection.peer()).thenReturn(peer);
        when(peer.remoteAddress()).thenReturn(new VirtualAddress("whatever"));
    }

    @SuppressWarnings({ "rawtypes", "unused" })
    private APIGatewayV2HTTPResponse mockHttpFunction(String query, DefaultFullHttpResponse httpResponse)
            throws ExecutionException, InterruptedException {
        when(request.getRawQueryString()).thenReturn(query);
        try (MockedStatic<Application> applicationMock = Mockito.mockStatic(Application.class)) {
            applicationMock.when(Application::currentApplication).thenReturn(application);
            LambdaHttpHandler lambda = new LambdaHttpHandler();
            CompletableFuture<APIGatewayV2HTTPResponse> requestFuture = CompletableFuture.supplyAsync(() -> {
                try (MockedStatic<VirtualClientConnection> connectionMock = Mockito.mockStatic(VirtualClientConnection.class)) {
                    connectionMock.when(() -> VirtualClientConnection.connect(any(), any(), any())).thenAnswer(i -> {
                        VirtualResponseHandler handler = i.getArgument(0);
                        CompletableFuture<Object> responseFuture = CompletableFuture.supplyAsync(() -> {
                            handler.handleMessage(httpResponse);
                            return null;
                        });
                        return connection;
                    });
                    return lambda.handleRequest(request, context);
                }
            });
            return requestFuture.get();
        }
    }

    public static Iterable<Object[]> queries() {
        return Arrays.asList(new Object[] { QUERY, PATH + "?" + QUERY }, new Object[] { "", PATH },
                new Object[] { null, PATH });
    }

    @ParameterizedTest
    @MethodSource("queries")
    public void verifyQueryParametersBypass(String query, String expected) throws ExecutionException, InterruptedException {
        mockHttpFunction(query, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(connection, timeout(PROCESSING_TIMEOUT).times(2)).sendMessage(captor.capture());
        DefaultHttpRequest rq = (DefaultHttpRequest) captor.getAllValues().get(0);
        assertEquals(expected, rq.uri());
    }

    public static Iterable<Object[]> responses() {
        return Arrays.asList(new Object[] { HttpResponseStatus.CREATED }, new Object[] { HttpResponseStatus.OK },
                new Object[] { HttpResponseStatus.BAD_REQUEST });
    }

    @ParameterizedTest
    @MethodSource("responses")
    public void verifyResponseStatusBypass(final HttpResponseStatus status) throws ExecutionException, InterruptedException {
        APIGatewayV2HTTPResponse response = mockHttpFunction(null,
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status));
        verify(connection, timeout(PROCESSING_TIMEOUT).times(2)).sendMessage(any());
        assertEquals(status.code(), response.getStatusCode());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Content-Type", "content-type" })
    public void verifyTextBodyIsNotBase64Encoded(String contentTypeHeaderName)
            throws ExecutionException, InterruptedException {
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer("hello", StandardCharsets.UTF_8));
        httpResponse.headers().set(contentTypeHeaderName, "text/plain");
        APIGatewayV2HTTPResponse response = mockHttpFunction(null, httpResponse);
        verify(connection, timeout(PROCESSING_TIMEOUT).times(2)).sendMessage(any());
        assertFalse(response.getIsBase64Encoded());
        assertEquals("hello", response.getBody());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Content-Type", "content-type" })
    public void verifyBinaryBodyIsBase64Encoded(String contentTypeHeaderName)
            throws ExecutionException, InterruptedException {
        byte[] bytes = new byte[] { 1, 2, 3 };
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer(bytes));
        httpResponse.headers().set(contentTypeHeaderName, "application/octet-stream");
        APIGatewayV2HTTPResponse response = mockHttpFunction(null, httpResponse);
        verify(connection, timeout(PROCESSING_TIMEOUT).times(2)).sendMessage(any());
        assertTrue(response.getIsBase64Encoded());
        assertEquals(Base64.getEncoder().encodeToString(bytes), response.getBody());
    }

    @Test
    public void verifyCookies() throws ExecutionException, InterruptedException {
        mockHttpFunction(null, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(connection, timeout(PROCESSING_TIMEOUT).times(2)).sendMessage(captor.capture());
        DefaultHttpRequest rq = (DefaultHttpRequest) captor.getAllValues().get(0);
        assertEquals(COOKIE_HEADER_VALUE, rq.headers().get(COOKIE_HEADER_KEY));
    }

}
