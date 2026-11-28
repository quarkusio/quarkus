package io.quarkus.vertx.http.runtime.filters.accesslog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.TooLongHttpHeaderException;
import io.vertx.core.Future;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.internal.http.HttpServerRequestInternal;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.SocketAddress;

class InvalidRequestAccessLogHandlerTest {

    @Test
    void testInvalidRequestIsLogged() {
        List<String> messages = new ArrayList<>();
        AccessLogReceiver receiver = messages::add;

        InvalidRequestAccessLogHandler handler = new InvalidRequestAccessLogHandler(receiver, "common",
                getClass().getClassLoader());

        HttpServerRequest request = createMockRequest("/test", new TooLongHttpHeaderException("too long"));
        handler.handle(request);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).contains("431").contains("GET /test HTTP/1.1");
    }

    @Test
    void testExcludePatternPreventsLogging() {
        List<String> messages = new ArrayList<>();
        AccessLogReceiver receiver = messages::add;

        InvalidRequestAccessLogHandler handler = new InvalidRequestAccessLogHandler(receiver, "common",
                getClass().getClassLoader(), Optional.of("^/excluded.*"));

        HttpServerRequest excludedRequest = createMockRequest("/excluded/path",
                new TooLongHttpHeaderException("too long"));
        handler.handle(excludedRequest);
        assertThat(messages).isEmpty();

        HttpServerRequest includedRequest = createMockRequest("/included/path",
                new TooLongHttpHeaderException("too long"));
        handler.handle(includedRequest);
        assertThat(messages).hasSize(1);
    }

    @Test
    void testDefensiveAgainstReceiverFailure() {
        AccessLogReceiver receiver = mock(AccessLogReceiver.class);
        doThrow(new RuntimeException("Receiver failed")).when(receiver).logMessage(anyString(), any());

        InvalidRequestAccessLogHandler handler = new InvalidRequestAccessLogHandler(receiver, "common",
                getClass().getClassLoader());

        HttpServerRequest request = createMockRequest("/test", new TooLongHttpHeaderException("too long"));

        assertThatCode(() -> handler.handle(request)).doesNotThrowAnyException();
        verify(request.response()).end();
        verify(request.connection()).close();
    }

    private HttpServerRequest createMockRequest(String path, Throwable cause) {
        HttpServerRequestInternal request = mock(HttpServerRequestInternal.class);
        HttpServerResponse response = mock(HttpServerResponse.class);
        HttpConnection connection = mock(HttpConnection.class);

        when(request.response()).thenReturn(response);
        when(request.connection()).thenReturn(connection);
        when(request.decoderResult()).thenReturn(DecoderResult.failure(cause));
        when(request.path()).thenReturn(path);
        when(request.uri()).thenReturn(path);
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.version()).thenReturn(HttpVersion.HTTP_1_1);
        when(request.scheme()).thenReturn("http");

        SocketAddress remoteAddress = mock(SocketAddress.class);
        when(remoteAddress.host()).thenReturn("127.0.0.1");
        when(request.remoteAddress()).thenReturn(remoteAddress);

        HostAndPort authority = mock(HostAndPort.class);
        when(authority.toString()).thenReturn("localhost:8080");
        when(request.authority()).thenReturn(authority);

        when(response.setStatusCode(any(Integer.class))).thenReturn(response);
        when(response.getStatusCode()).thenReturn(431);
        when(response.end()).thenReturn(Future.succeededFuture());
        when(connection.close()).thenReturn(Future.succeededFuture());

        return request;
    }
}
