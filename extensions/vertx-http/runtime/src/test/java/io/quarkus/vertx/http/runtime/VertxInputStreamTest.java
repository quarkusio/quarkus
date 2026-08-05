package io.quarkus.vertx.http.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.netty.channel.Channel;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.ext.web.RoutingContext;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VertxInputStreamTest {

    @Mock
    RoutingContext routingContext;

    @Mock
    HttpServerRequest request;

    @Mock
    HttpServerResponse response;

    @Mock(extraInterfaces = HttpConnection.class)
    ConnectionBase connection;

    @Mock
    Channel channel;

    @BeforeEach
    void setUp() {
        when(routingContext.request()).thenReturn(request);
        when(request.connection()).thenReturn((HttpConnection) connection);
        when(request.response()).thenReturn(response);
        when(connection.channel()).thenReturn(channel);
        when(channel.isOpen()).thenReturn(true);
        when(request.isEnded()).thenReturn(true); // avoid handler setup complexity
    }

    @Test
    void readSingleByteFromExistingBuffer() throws IOException {
        Buffer buf = Buffer.buffer(new byte[] { 0x42 });
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);

        int result = stream.read();
        assertThat(result).isEqualTo(0x42);

        int eof = stream.read();
        assertThat(eof).isEqualTo(-1);
    }

    @Test
    void readFullArrayFromExistingBuffer() throws IOException {
        Buffer buf = Buffer.buffer("hello");
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);

        byte[] out = new byte[5];
        int read = stream.read(out);
        assertThat(read).isEqualTo(5);
        assertThat(out).isEqualTo("hello".getBytes());
    }

    @Test
    void readPartialThenRemainder() throws IOException {
        Buffer buf = Buffer.buffer("abcdef");
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);

        byte[] part1 = new byte[3];
        int read1 = stream.read(part1, 0, 3);
        assertThat(read1).isEqualTo(3);
        assertThat(part1).isEqualTo("abc".getBytes());

        byte[] part2 = new byte[3];
        int read2 = stream.read(part2, 0, 3);
        assertThat(read2).isEqualTo(3);
        assertThat(part2).isEqualTo("def".getBytes());

        int eof = stream.read();
        assertThat(eof).isEqualTo(-1);
    }

    @Test
    void readWithLenZeroReturnsZero() throws IOException {
        Buffer buf = Buffer.buffer(new byte[] { 1, 2, 3 });
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);

        byte[] out = new byte[3];
        int read = stream.read(out, 0, 0);
        assertThat(read).isEqualTo(0);

        // Data should still be available
        read = stream.read(out);
        assertThat(read).isEqualTo(3);
        assertThat(out).isEqualTo(new byte[] { 1, 2, 3 });
    }

    @Test
    void readHighByteValue() throws IOException {
        Buffer buf = Buffer.buffer(new byte[] { (byte) 0xFF });
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);

        int result = stream.read();
        assertThat(result).isEqualTo(0xFF);
    }

    @Test
    void readOnClosedStreamThrows() throws IOException {
        Buffer buf = Buffer.buffer("data");
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);
        stream.close();

        assertThatThrownBy(stream::read)
                .isInstanceOf(IOException.class)
                .hasMessage("Stream is closed");
    }

    @Test
    void availableOnClosedStreamThrows() throws IOException {
        Buffer buf = Buffer.buffer("data");
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);
        stream.close();

        assertThatThrownBy(stream::available)
                .isInstanceOf(IOException.class)
                .hasMessage("Stream is closed");
    }

    @Test
    void closeIsIdempotent() throws IOException {
        Buffer buf = Buffer.buffer("data");
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);
        stream.close();
        stream.close(); // should not throw
    }

    @Test
    void availableReturnsZeroWhenFinished() throws IOException {
        Buffer buf = Buffer.buffer("hi");
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);

        // Read all data
        byte[] out = new byte[2];
        stream.read(out);
        // Read once more to trigger finished (returns -1)
        stream.read();

        assertThat(stream.available()).isEqualTo(0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void continueHeaderTriggersContinueResponse() throws IOException {
        // Use 2-arg constructor which checks Expect header
        when(request.getHeader(io.netty.handler.codec.http.HttpHeaderNames.EXPECT)).thenReturn("100-continue");
        when(request.isEnded()).thenReturn(false);
        when(request.pause()).thenReturn(request);
        when(request.handler(any())).thenReturn(request);
        when(request.endHandler(any())).thenReturn(request);
        when(request.exceptionHandler(any())).thenReturn(request);
        when(request.fetch(1)).thenReturn(request);

        VertxInputStream stream = new VertxInputStream(routingContext, 10000);

        // Capture the data handler to feed data
        ArgumentCaptor<Handler<Buffer>> handlerCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(request).handler(handlerCaptor.capture());
        Handler<Buffer> dataHandler = handlerCaptor.getValue();

        // Feed data through handler (must be done in synchronized block on connection)
        synchronized (connection) {
            dataHandler.handle(Buffer.buffer("test"));
        }

        // Read triggers continueState -> writeContinue
        byte[] out = new byte[4];
        stream.read(out);

        verify(response).writeContinue();
        assertThat(out).isEqualTo("test".getBytes());
    }

    @Test
    void noContinueWithoutExpectHeader() throws IOException {
        // Use 3-arg constructor (with existing buffer) - no continueState set up
        Buffer buf = Buffer.buffer("data");
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);

        byte[] out = new byte[4];
        stream.read(out);

        verify(response, never()).writeContinue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void requestTooLargeHeadNotWritten() throws IOException {
        when(routingContext.<Long> get(VertxHttpRecorder.MAX_REQUEST_SIZE_KEY)).thenReturn(10L);
        when(request.bytesRead()).thenReturn(100L);
        when(response.headWritten()).thenReturn(false);
        MultiMap headers = HttpHeaders.headers();
        when(response.headers()).thenReturn(headers);
        when(response.setStatusCode(413)).thenReturn(response);
        when(response.endHandler(any())).thenReturn(response);

        Buffer buf = Buffer.buffer("data");
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);

        assertThatThrownBy(() -> stream.read(new byte[4]))
                .isInstanceOf(IOException.class)
                .hasMessage("Request too large");

        verify(response).setStatusCode(413);
    }

    @Test
    void requestTooLargeHeadAlreadyWritten() throws IOException {
        when(routingContext.<Long> get(VertxHttpRecorder.MAX_REQUEST_SIZE_KEY)).thenReturn(10L);
        when(request.bytesRead()).thenReturn(100L);
        when(response.headWritten()).thenReturn(true);

        Buffer buf = Buffer.buffer("data");
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);

        assertThatThrownBy(() -> stream.read(new byte[4]))
                .isInstanceOf(IOException.class)
                .hasMessage("Request too large");

        verify(connection).close();
    }

    @Test
    void closedChannelOnConstructionPropagatesException() throws IOException {
        when(channel.isOpen()).thenReturn(false);
        when(request.isEnded()).thenReturn(false);

        Buffer buf = Buffer.buffer("initial");
        VertxInputStream stream = new VertxInputStream(routingContext, 10000, buf);

        // First read consumes existing buffer
        byte[] out = new byte[7];
        int read = stream.read(out);
        assertThat(read).isEqualTo(7);
        assertThat(out).isEqualTo("initial".getBytes());

        // Second read triggers readBlocking which sees readException (ClosedChannelException)
        assertThatThrownBy(() -> stream.read(new byte[1]))
                .isInstanceOf(IOException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    void http2EmptyFrameTriggersEof() throws IOException {
        when(request.isEnded()).thenReturn(false);
        when(request.pause()).thenReturn(request);
        when(request.handler(any())).thenReturn(request);
        when(request.endHandler(any())).thenReturn(request);
        when(request.exceptionHandler(any())).thenReturn(request);
        when(request.fetch(1)).thenReturn(request);
        when(request.version()).thenReturn(HttpVersion.HTTP_2);

        VertxInputStream.VertxBlockingInput input = new VertxInputStream.VertxBlockingInput(request, 10000);

        // Feed an empty buffer (HTTP/2 signals EOF)
        synchronized (connection) {
            input.handle(Buffer.buffer(new byte[0]));
        }

        io.netty.buffer.ByteBuf result = input.readBlocking();
        assertThat(result).isNull();
    }

    @Test
    void readBytesAvailableWithContentLengthHeader() {
        when(request.getHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn("42");

        VertxInputStream.VertxBlockingInput input = new VertxInputStream.VertxBlockingInput(request, 10000);

        assertThat(input.readBytesAvailable()).isEqualTo(42);
    }

    @Test
    void readBytesAvailableWithNoContentLength() {
        when(request.getHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(null);

        VertxInputStream.VertxBlockingInput input = new VertxInputStream.VertxBlockingInput(request, 10000);

        assertThat(input.readBytesAvailable()).isEqualTo(0);
    }

    @Test
    void readBytesAvailableWithLargeContentLength() {
        when(request.getHeader(HttpHeaders.CONTENT_LENGTH)).thenReturn(String.valueOf(Long.MAX_VALUE));

        VertxInputStream.VertxBlockingInput input = new VertxInputStream.VertxBlockingInput(request, 10000);

        assertThat(input.readBytesAvailable()).isEqualTo(Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    @Test
    void multipleChunksViaHandle() throws IOException {
        when(request.isEnded()).thenReturn(false);
        when(request.pause()).thenReturn(request);
        when(request.handler(any())).thenReturn(request);
        when(request.endHandler(any())).thenReturn(request);
        when(request.exceptionHandler(any())).thenReturn(request);
        when(request.fetch(1)).thenReturn(request);

        VertxInputStream.VertxBlockingInput input = new VertxInputStream.VertxBlockingInput(request, 10000);

        // Feed two chunks
        synchronized (connection) {
            input.handle(Buffer.buffer("abc"));
            input.handle(Buffer.buffer("def"));
        }

        io.netty.buffer.ByteBuf chunk1 = input.readBlocking();
        assertThat(chunk1).isNotNull();
        byte[] bytes1 = new byte[chunk1.readableBytes()];
        chunk1.readBytes(bytes1);
        assertThat(bytes1).isEqualTo("abc".getBytes());
        chunk1.release();

        io.netty.buffer.ByteBuf chunk2 = input.readBlocking();
        assertThat(chunk2).isNotNull();
        byte[] bytes2 = new byte[chunk2.readableBytes()];
        chunk2.readBytes(bytes2);
        assertThat(bytes2).isEqualTo("def".getBytes());
        chunk2.release();
    }
}
