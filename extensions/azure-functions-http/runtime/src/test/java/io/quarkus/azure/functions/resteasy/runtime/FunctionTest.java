package io.quarkus.azure.functions.resteasy.runtime;

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.netty.runtime.virtual.VirtualResponseHandler;
import io.quarkus.runtime.Application;

@SuppressWarnings("unchecked")
public class FunctionTest {
    private static final long PROCESSING_TIMEOUT = TimeUnit.SECONDS.toMillis(1);

    private static final String PATH = "/test/path";
    private static final String QUERY = "testParam1=testValue1&testParam2=testValue2";
    private static final String HOST_HEADER = "Host";
    private static final String HOST = "localhost";
    private static final HttpMethod METHOD = HttpMethod.GET;

    private final Application application = mock(Application.class);
    private final HttpRequestMessage<Optional<String>> request = mock(HttpRequestMessage.class);
    private final ExecutionContext context = mock(ExecutionContext.class);
    private final VirtualClientConnection<?> connection = mock(VirtualClientConnection.class);
    private final HttpResponseMessage.Builder responseBuilder = mock(HttpResponseMessage.Builder.class);
    private final HttpResponseMessage response = mock(HttpResponseMessage.class);

    static {
        BaseFunction.throwException = false;
    }

    @BeforeEach
    public void mockSetup() {
        when(request.getHttpMethod()).thenReturn(METHOD);
        when(request.getHeaders()).thenReturn(Collections.singletonMap(HOST_HEADER, HOST));
        when(request.getBody()).thenReturn(Optional.empty());
        when(request.createResponseBuilder(any(HttpStatus.class))).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);
    }

    @SuppressWarnings({ "rawtypes", "unused" })
    private void mockHttpFunction(String query, HttpResponseStatus status)
            throws ExecutionException, InterruptedException, URISyntaxException {
        URI requestUri = new URI("http://" + HOST + PATH + ofNullable(query).map(q -> "?" + q).orElse(""));
        when(request.getUri()).thenReturn(requestUri);
        CompletableFuture<HttpResponseMessage> requestFuture = CompletableFuture.supplyAsync(() -> {
            try (MockedStatic<Application> applicationMock = Mockito.mockStatic(Application.class)) {
                applicationMock.when(Application::currentApplication).thenReturn(application);
                Function function = new Function();
                try (MockedStatic<VirtualClientConnection> connectionMock = Mockito
                        .mockStatic(VirtualClientConnection.class)) {
                    connectionMock.when(() -> VirtualClientConnection.connect(any(), any())).thenAnswer(i -> {
                        VirtualResponseHandler handler = i.getArgument(0);
                        CompletableFuture<Object> responseFuture = CompletableFuture.supplyAsync(() -> {
                            handler.handleMessage(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status));
                            return null;
                        });
                        return connection;
                    });
                    return function.run(request, context);
                }
            }
        });
        requestFuture.get();
    }

    public static Iterable<Object[]> queries() {
        return Arrays.asList(new Object[] { QUERY, PATH + "?" + QUERY }, new Object[] { null, PATH });
    }

    @ParameterizedTest
    @MethodSource("queries")
    public void verifyQueryParametersBypass(String query, String expected)
            throws ExecutionException, InterruptedException, URISyntaxException {
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
    public void verifyResponseStatusBypass(final HttpResponseStatus status)
            throws ExecutionException, InterruptedException, URISyntaxException {
        mockHttpFunction(null, status);
        verify(connection, timeout(PROCESSING_TIMEOUT).times(2)).sendMessage(any());
        ArgumentCaptor<HttpStatus> statusCaptor = ArgumentCaptor.forClass(HttpStatus.class);
        verify(request, timeout(PROCESSING_TIMEOUT)).createResponseBuilder(statusCaptor.capture());
        assertEquals(status.code(), statusCaptor.getValue().value());
    }
}
