package io.quarkus.amazon.lambda.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.netty.runtime.virtual.VirtualResponseHandler;
import io.quarkus.runtime.Application;

public class LambdaHttpHandlerTest {

    private static final long PROCESSING_TIMEOUT = TimeUnit.SECONDS.toMillis(1);

    private static final String PATH = "/test/path";
    private static final String QUERY = "testParam1=testValue1&testParam2=testValue2";
    private static final String HOST_HEADER = "Host";
    private static final String HOST = "localhost";
    private static final String METHOD = "GET";

    private final Application application = mock(Application.class);
    private final APIGatewayV2HTTPEvent request = mock(APIGatewayV2HTTPEvent.class);
    private final APIGatewayV2HTTPEvent.RequestContext requestContext = mock(APIGatewayV2HTTPEvent.RequestContext.class);
    private final APIGatewayV2HTTPEvent.RequestContext.Http requestContextMethod = mock(
            APIGatewayV2HTTPEvent.RequestContext.Http.class);
    private final Context context = mock(Context.class);
    private final VirtualClientConnection<?> connection = mock(VirtualClientConnection.class);

    @BeforeEach
    public void mockSetup() {
        when(request.getRawPath()).thenReturn(PATH);
        when(request.getRequestContext()).thenReturn(requestContext);
        when(requestContext.getHttp()).thenReturn(requestContextMethod);
        when(requestContextMethod.getMethod()).thenReturn(METHOD);
        when(request.getHeaders()).thenReturn(Collections.singletonMap(HOST_HEADER, HOST));
    }

    @SuppressWarnings({ "rawtypes", "unused" })
    private APIGatewayV2HTTPResponse mockHttpFunction(String query, HttpResponseStatus status)
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
                            handler.handleMessage(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status));
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
        mockHttpFunction(query, HttpResponseStatus.OK);
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
        APIGatewayV2HTTPResponse response = mockHttpFunction(null, status);
        verify(connection, timeout(PROCESSING_TIMEOUT).times(2)).sendMessage(any());
        assertEquals(status.code(), response.getStatusCode());
    }

}
