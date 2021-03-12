package io.quarkus.gcp.functions.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.quarkus.netty.runtime.virtual.VirtualClientConnection;
import io.quarkus.netty.runtime.virtual.VirtualResponseHandler;
import io.quarkus.runtime.Application;

public class QuarkusHttpFunctionTest {
    private static final long PROCESSING_TIMEOUT = TimeUnit.SECONDS.toMillis(1);

    private static final String PATH = "/test/path";
    private static final String QUERY = "testParam1=testValue1&testParam2=testValue2";
    private static final String HOST_HEADER = "Host";
    private static final String HOST = "localhost";
    private static final String METHOD = "GET";

    private final Application application = mock(Application.class);
    private final HttpRequest request = mock(HttpRequest.class);
    private final HttpResponse response = mock(HttpResponse.class);
    private final VirtualClientConnection<?> connection = mock(VirtualClientConnection.class);

    @BeforeEach
    public void mockSetup() {
        when(request.getPath()).thenReturn(PATH);
        when(request.getFirstHeader(eq(HOST_HEADER))).thenReturn(Optional.of(HOST));
        when(request.getHeaders()).thenReturn(Collections.singletonMap(HOST_HEADER, Collections.singletonList(HOST)));
        when(request.getMethod()).thenReturn(METHOD);
    }

    @SuppressWarnings({ "OptionalUsedAsFieldOrParameterType", "rawtypes", "unused" })
    private void mockHttpFunction(Optional<String> query, HttpResponseStatus status) {
        when(request.getQuery()).thenReturn(query);
        try (MockedStatic<Application> applicationMock = Mockito.mockStatic(Application.class)) {
            applicationMock.when(Application::currentApplication).thenReturn(application);
            QuarkusHttpFunction function = new QuarkusHttpFunction();
            CompletableFuture<Void> requestFuture = CompletableFuture.supplyAsync(() -> {
                try (MockedStatic<VirtualClientConnection> connectionMock = Mockito.mockStatic(VirtualClientConnection.class)) {
                    connectionMock.when(() -> VirtualClientConnection.connect(any(), any())).thenAnswer(i -> {
                        VirtualResponseHandler handler = i.getArgument(0);
                        CompletableFuture<Object> responseFuture = CompletableFuture.supplyAsync(() -> {
                            handler.handleMessage(new DefaultHttpResponse(HttpVersion.HTTP_1_1, status));
                            return null;
                        });
                        return connection;
                    });
                    function.service(request, response);
                } catch (IOException ignore) {
                }
                return null;
            });
        }
    }

    public static Iterable<Object[]> queries() {
        return Arrays.asList(new Object[] { Optional.of(QUERY), PATH + "?" + QUERY }, new Object[] { Optional.empty(), PATH });
    }

    @ParameterizedTest
    @MethodSource("queries")
    @SuppressWarnings({ "OptionalUsedAsFieldOrParameterType" })
    public void verifyQueryParametersBypass(Optional<String> query, String expected) {
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
    public void verifyResponseStatusBypass(final HttpResponseStatus status) {
        mockHttpFunction(Optional.empty(), status);
        verify(connection, timeout(PROCESSING_TIMEOUT).times(2)).sendMessage(any());
        verify(response, timeout(PROCESSING_TIMEOUT)).setStatusCode(eq(status.code()), eq(status.reasonPhrase()));
    }
}
