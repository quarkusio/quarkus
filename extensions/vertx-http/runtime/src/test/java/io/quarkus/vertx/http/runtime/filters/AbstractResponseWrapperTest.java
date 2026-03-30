package io.quarkus.vertx.http.runtime.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.ReadStream;

@ExtendWith(MockitoExtension.class)
public class AbstractResponseWrapperTest {

    @Mock
    HttpServerResponse delegate;

    private AbstractResponseWrapper wrapper;

    @BeforeEach
    void setUp() {
        wrapper = new AbstractResponseWrapper(delegate) {
        };
    }

    @Test
    void getStatusCode_delegates() {
        when(delegate.getStatusCode()).thenReturn(404);
        assertEquals(404, wrapper.getStatusCode());
        verify(delegate).getStatusCode();
    }

    @Test
    void setStatusCode_delegatesAndReturnsThis() {
        HttpServerResponse result = wrapper.setStatusCode(201);
        verify(delegate).setStatusCode(201);
        assertSame(wrapper, result);
    }

    @Test
    void getStatusMessage_delegates() {
        when(delegate.getStatusMessage()).thenReturn("Not Found");
        assertEquals("Not Found", wrapper.getStatusMessage());
        verify(delegate).getStatusMessage();
    }

    @Test
    void setStatusMessage_delegatesAndReturnsThis() {
        HttpServerResponse result = wrapper.setStatusMessage("OK");
        verify(delegate).setStatusMessage("OK");
        assertSame(wrapper, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void writeBuffer_delegatesAndReturnsFuture() {
        Buffer buf = mock(Buffer.class);
        Future<Void> future = mock(Future.class);
        when(delegate.write(buf)).thenReturn(future);

        assertSame(future, wrapper.write(buf));
        verify(delegate).write(buf);
    }

    @Test
    @SuppressWarnings("unchecked")
    void writeBufferWithHandler_delegates() {
        Buffer buf = mock(Buffer.class);
        Handler<AsyncResult<Void>> handler = mock(Handler.class);

        wrapper.write(buf, handler);
        verify(delegate).write(buf, handler);
    }

    @Test
    @SuppressWarnings("unchecked")
    void writeString_delegatesAndReturnsFuture() {
        Future<Void> future = mock(Future.class);
        when(delegate.write("hello")).thenReturn(future);

        assertSame(future, wrapper.write("hello"));
        verify(delegate).write("hello");
    }

    @Test
    @SuppressWarnings("unchecked")
    void writeStringWithEncoding_delegatesAndReturnsFuture() {
        Future<Void> future = mock(Future.class);
        when(delegate.write("hello", "UTF-8")).thenReturn(future);

        assertSame(future, wrapper.write("hello", "UTF-8"));
        verify(delegate).write("hello", "UTF-8");
    }

    @Test
    @SuppressWarnings("unchecked")
    void end_delegatesAndReturnsFuture() {
        Future<Void> future = mock(Future.class);
        when(delegate.end()).thenReturn(future);

        assertSame(future, wrapper.end());
        verify(delegate).end();
    }

    @Test
    @SuppressWarnings("unchecked")
    void endWithHandler_delegates() {
        Handler<AsyncResult<Void>> handler = mock(Handler.class);
        wrapper.end(handler);
        verify(delegate).end(handler);
    }

    @Test
    @SuppressWarnings("unchecked")
    void endString_delegatesAndReturnsFuture() {
        Future<Void> future = mock(Future.class);
        when(delegate.end("bye")).thenReturn(future);

        assertSame(future, wrapper.end("bye"));
        verify(delegate).end("bye");
    }

    @Test
    @SuppressWarnings("unchecked")
    void endBuffer_delegatesAndReturnsFuture() {
        Buffer buf = mock(Buffer.class);
        Future<Void> future = mock(Future.class);
        when(delegate.end(buf)).thenReturn(future);

        assertSame(future, wrapper.end(buf));
        verify(delegate).end(buf);
    }

    @Test
    @SuppressWarnings("unchecked")
    void endStringEncoding_delegatesAndReturnsFuture() {
        Future<Void> future = mock(Future.class);
        when(delegate.end("bye", "UTF-8")).thenReturn(future);

        assertSame(future, wrapper.end("bye", "UTF-8"));
        verify(delegate).end("bye", "UTF-8");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendNoArgs_delegatesAndReturnsFuture() {
        Future<Void> future = mock(Future.class);
        when(delegate.send()).thenReturn(future);

        assertSame(future, wrapper.send());
        verify(delegate).send();
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendString_delegatesAndReturnsFuture() {
        Future<Void> future = mock(Future.class);
        when(delegate.send("body")).thenReturn(future);

        assertSame(future, wrapper.send("body"));
        verify(delegate).send("body");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendBuffer_delegatesAndReturnsFuture() {
        Buffer buf = mock(Buffer.class);
        Future<Void> future = mock(Future.class);
        when(delegate.send(buf)).thenReturn(future);

        assertSame(future, wrapper.send(buf));
        verify(delegate).send(buf);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendReadStream_delegatesAndReturnsFuture() {
        ReadStream<Buffer> stream = mock(ReadStream.class);
        Future<Void> future = mock(Future.class);
        when(delegate.send(stream)).thenReturn(future);

        assertSame(future, wrapper.send(stream));
        verify(delegate).send(stream);
    }

    @Test
    void headers_delegates() {
        MultiMap headers = mock(MultiMap.class);
        when(delegate.headers()).thenReturn(headers);

        assertSame(headers, wrapper.headers());
        verify(delegate).headers();
    }

    @Test
    void putHeaderStringString_delegatesAndReturnsThis() {
        HttpServerResponse result = wrapper.putHeader("Content-Type", "text/plain");
        verify(delegate).putHeader("Content-Type", "text/plain");
        assertSame(wrapper, result);
    }

    @Test
    void putHeaderCharSequence_delegatesAndReturnsThis() {
        CharSequence name = "X-Custom";
        CharSequence value = "val";
        HttpServerResponse result = wrapper.putHeader(name, value);
        verify(delegate).putHeader(name, value);
        assertSame(wrapper, result);
    }

    @Test
    void trailers_delegates() {
        MultiMap trailers = mock(MultiMap.class);
        when(delegate.trailers()).thenReturn(trailers);

        assertSame(trailers, wrapper.trailers());
        verify(delegate).trailers();
    }

    @Test
    void putTrailer_delegatesAndReturnsThis() {
        HttpServerResponse result = wrapper.putTrailer("Trailer-Name", "trailer-value");
        verify(delegate).putTrailer("Trailer-Name", "trailer-value");
        assertSame(wrapper, result);
    }

    @Test
    void ended_delegates() {
        when(delegate.ended()).thenReturn(true);
        assertTrue(wrapper.ended());
        verify(delegate).ended();
    }

    @Test
    void closed_delegates() {
        when(delegate.closed()).thenReturn(false);
        assertFalse(wrapper.closed());
        verify(delegate).closed();
    }

    @Test
    void headWritten_delegates() {
        when(delegate.headWritten()).thenReturn(true);
        assertTrue(wrapper.headWritten());
        verify(delegate).headWritten();
    }

    @Test
    void isChunked_delegates() {
        when(delegate.isChunked()).thenReturn(true);
        assertTrue(wrapper.isChunked());
        verify(delegate).isChunked();
    }

    @Test
    void setChunked_delegatesAndReturnsThis() {
        HttpServerResponse result = wrapper.setChunked(true);
        verify(delegate).setChunked(true);
        assertSame(wrapper, result);
    }

    @Test
    void bytesWritten_delegates() {
        when(delegate.bytesWritten()).thenReturn(42L);
        assertEquals(42L, wrapper.bytesWritten());
        verify(delegate).bytesWritten();
    }

    @Test
    void streamId_delegates() {
        when(delegate.streamId()).thenReturn(7);
        assertEquals(7, wrapper.streamId());
        verify(delegate).streamId();
    }

    @Test
    void writeQueueFull_delegates() {
        when(delegate.writeQueueFull()).thenReturn(true);
        assertTrue(wrapper.writeQueueFull());
        verify(delegate).writeQueueFull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void exceptionHandler_delegatesAndReturnsThis() {
        Handler<Throwable> handler = mock(Handler.class);
        HttpServerResponse result = wrapper.exceptionHandler(handler);
        verify(delegate).exceptionHandler(handler);
        assertSame(wrapper, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void drainHandler_delegatesAndReturnsThis() {
        Handler<Void> handler = mock(Handler.class);
        HttpServerResponse result = wrapper.drainHandler(handler);
        verify(delegate).drainHandler(handler);
        assertSame(wrapper, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void closeHandler_delegatesAndReturnsThis() {
        Handler<Void> handler = mock(Handler.class);
        HttpServerResponse result = wrapper.closeHandler(handler);
        verify(delegate).closeHandler(handler);
        assertSame(wrapper, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void endHandler_delegatesAndReturnsThis() {
        Handler<Void> handler = mock(Handler.class);
        HttpServerResponse result = wrapper.endHandler(handler);
        verify(delegate).endHandler(handler);
        assertSame(wrapper, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void headersEndHandler_delegatesAndReturnsThis() {
        Handler<Void> handler = mock(Handler.class);
        HttpServerResponse result = wrapper.headersEndHandler(handler);
        verify(delegate).headersEndHandler(handler);
        assertSame(wrapper, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void bodyEndHandler_delegatesAndReturnsThis() {
        Handler<Void> handler = mock(Handler.class);
        HttpServerResponse result = wrapper.bodyEndHandler(handler);
        verify(delegate).bodyEndHandler(handler);
        assertSame(wrapper, result);
    }

    @Test
    void addCookie_delegatesAndReturnsThis() {
        Cookie cookie = mock(Cookie.class);
        HttpServerResponse result = wrapper.addCookie(cookie);
        verify(delegate).addCookie(cookie);
        assertSame(wrapper, result);
    }

    @Test
    void removeCookieByName_delegates() {
        Cookie cookie = mock(Cookie.class);
        when(delegate.removeCookie("session")).thenReturn(cookie);

        assertSame(cookie, wrapper.removeCookie("session"));
        verify(delegate).removeCookie("session");
    }

    @Test
    void removeCookieByNameAndInvalidate_delegates() {
        Cookie cookie = mock(Cookie.class);
        when(delegate.removeCookie("session", false)).thenReturn(cookie);

        assertSame(cookie, wrapper.removeCookie("session", false));
        verify(delegate).removeCookie("session", false);
    }

    @Test
    @SuppressWarnings("unchecked")
    void removeCookies_delegates() {
        Set<Cookie> cookies = mock(Set.class);
        when(delegate.removeCookies("session")).thenReturn(cookies);

        assertSame(cookies, wrapper.removeCookies("session"));
        verify(delegate).removeCookies("session");
    }

    @Test
    void removeCookieWithDomainAndPath_delegates() {
        Cookie cookie = mock(Cookie.class);
        when(delegate.removeCookie("name", "domain.com", "/path")).thenReturn(cookie);

        assertSame(cookie, wrapper.removeCookie("name", "domain.com", "/path"));
        verify(delegate).removeCookie("name", "domain.com", "/path");
    }

    @Test
    void setWriteQueueMaxSize_delegatesAndReturnsThis() {
        HttpServerResponse result = wrapper.setWriteQueueMaxSize(1024);
        verify(delegate).setWriteQueueMaxSize(1024);
        assertSame(wrapper, result);
    }

    @Test
    void writeContinue_delegatesAndReturnsThis() {
        HttpServerResponse result = wrapper.writeContinue();
        verify(delegate).writeContinue();
        assertSame(wrapper, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendFile_delegatesAndReturnsFuture() {
        Future<Void> future = mock(Future.class);
        when(delegate.sendFile("file.txt")).thenReturn(future);

        assertSame(future, wrapper.sendFile("file.txt"));
        verify(delegate).sendFile("file.txt");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendFileWithHandler_delegatesAndReturnsThis() {
        Handler<AsyncResult<Void>> handler = mock(Handler.class);
        HttpServerResponse result = wrapper.sendFile("file.txt", handler);
        verify(delegate).sendFile("file.txt", handler);
        assertSame(wrapper, result);
    }

    @Test
    @SuppressWarnings("deprecation")
    void close_delegates() {
        wrapper.close();
        verify(delegate).close();
    }

    @Test
    void reset_delegates() {
        when(delegate.reset()).thenReturn(true);
        assertTrue(wrapper.reset());
        verify(delegate).reset();
    }
}
