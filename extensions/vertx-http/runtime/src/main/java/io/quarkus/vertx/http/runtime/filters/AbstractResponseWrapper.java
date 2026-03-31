package io.quarkus.vertx.http.runtime.filters;

import java.util.Set;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.StreamPriority;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.streams.ReadStream;

public class AbstractResponseWrapper implements HttpServerResponse {

    private final HttpServerResponse delegate;

    protected AbstractResponseWrapper(HttpServerResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
        delegate.exceptionHandler(handler);
        return this;
    }

    @Override
    public Future<Void> write(Buffer data) {
        return delegate.write(data);
    }

    @Override
    public HttpServerResponse setWriteQueueMaxSize(int maxSize) {
        delegate.setWriteQueueMaxSize(maxSize);
        return this;
    }

    @Override
    public HttpServerResponse drainHandler(Handler<Void> handler) {
        delegate.drainHandler(handler);
        return this;
    }

    @Override
    public int getStatusCode() {
        return delegate.getStatusCode();
    }

    @Override

    public HttpServerResponse setStatusCode(int statusCode) {
        delegate.setStatusCode(statusCode);
        return this;
    }

    @Override
    public String getStatusMessage() {
        return delegate.getStatusMessage();
    }

    @Override

    public HttpServerResponse setStatusMessage(String statusMessage) {
        delegate.setStatusMessage(statusMessage);
        return this;
    }

    @Override

    public HttpServerResponse setChunked(boolean chunked) {
        delegate.setChunked(chunked);
        return this;
    }

    @Override
    public boolean isChunked() {
        return delegate.isChunked();
    }

    @Override

    public MultiMap headers() {
        return delegate.headers();
    }

    @Override

    public HttpServerResponse putHeader(String name, String value) {
        delegate.putHeader(name, value);
        return this;
    }

    @Override

    public HttpServerResponse putHeader(CharSequence name, CharSequence value) {
        delegate.putHeader(name, value);
        return this;
    }

    @Override

    public HttpServerResponse putHeader(String name, Iterable<String> values) {
        delegate.putHeader(name, values);
        return this;
    }

    @Override

    public HttpServerResponse putHeader(CharSequence name, Iterable<CharSequence> values) {
        delegate.putHeader(name, values);
        return this;
    }

    @Override

    public MultiMap trailers() {
        return delegate.trailers();
    }

    @Override

    public HttpServerResponse putTrailer(String name, String value) {
        delegate.putTrailer(name, value);
        return this;
    }

    @Override

    public HttpServerResponse putTrailer(CharSequence name, CharSequence value) {
        delegate.putTrailer(name, value);
        return this;
    }

    @Override

    public HttpServerResponse putTrailer(String name, Iterable<String> values) {
        delegate.putTrailer(name, values);
        return this;
    }

    @Override

    public HttpServerResponse putTrailer(CharSequence name, Iterable<CharSequence> value) {
        delegate.putTrailer(name, value);
        return this;
    }

    @Override

    public HttpServerResponse closeHandler(Handler<Void> handler) {
        delegate.closeHandler(handler);
        return this;
    }

    @Override

    public HttpServerResponse endHandler(Handler<Void> handler) {
        delegate.endHandler(handler);
        return this;
    }

    @Override
    public Future<Void> writeHead() {
        return delegate.writeHead();
    }

    @Override

    public Future<Void> write(String chunk, String enc) {
        return delegate.write(chunk, enc);
    }

    @Override

    public Future<Void> write(String chunk) {
        return delegate.write(chunk);
    }

    @Override

    public Future<Void> writeContinue() {
        return delegate.writeContinue();
    }

    @Override
    public Future<Void> writeEarlyHints(MultiMap headers) {
        return delegate.writeEarlyHints(headers);
    }

    @Override
    public Future<Void> end(String chunk) {
        return delegate.end(chunk);
    }

    @Override
    public Future<Void> end(String chunk, String enc) {
        return delegate.end(chunk, enc);
    }

    @Override
    public Future<Void> end(Buffer chunk) {
        return delegate.end(chunk);
    }

    @Override
    public Future<Void> end() {
        return delegate.end();
    }

    @Override
    public Future<Void> send() {
        return delegate.send();
    }

    @Override
    public Future<Void> send(String body) {
        return delegate.send(body);
    }

    @Override
    public Future<Void> send(Buffer body) {
        return delegate.send(body);
    }

    @Override
    public Future<Void> send(ReadStream<Buffer> body) {
        return delegate.send(body);
    }

    @Override
    public Future<Void> sendFile(String filename) {
        return delegate.sendFile(filename);
    }

    @Override

    public Future<Void> sendFile(String filename, long offset) {
        return delegate.sendFile(filename, offset);
    }

    @Override

    public Future<Void> sendFile(String filename, long offset, long length) {
        return delegate.sendFile(filename, offset, length);
    }

    @Override
    public Future<Void> sendFile(java.io.RandomAccessFile file, long offset, long length) {
        return delegate.sendFile(file, offset, length);
    }

    @Override
    public Future<Void> sendFile(java.nio.channels.FileChannel file, long offset, long length) {
        return delegate.sendFile(file, offset, length);
    }

    @Override
    public boolean ended() {
        return delegate.ended();
    }

    @Override
    public boolean closed() {
        return delegate.closed();
    }

    @Override
    public boolean headWritten() {
        return delegate.headWritten();
    }

    @Override

    public HttpServerResponse headersEndHandler(Handler<Void> handler) {
        delegate.headersEndHandler(handler);
        return this;
    }

    @Override

    public HttpServerResponse bodyEndHandler(Handler<Void> handler) {
        delegate.bodyEndHandler(handler);
        return this;
    }

    @Override
    public long bytesWritten() {
        return delegate.bytesWritten();
    }

    @Override
    public int streamId() {
        return delegate.streamId();
    }

    @Override
    public Future<HttpServerResponse> push(HttpMethod method, HostAndPort authority, String path, MultiMap headers) {
        return delegate.push(method, authority, path, headers);
    }

    @Override
    public Future<Void> reset() {
        return delegate.reset();
    }

    @Override
    public Future<Void> reset(long code) {
        return delegate.reset(code);
    }

    @Override

    public Future<Void> writeCustomFrame(int type, int flags, Buffer payload) {
        return delegate.writeCustomFrame(type, flags, payload);
    }

    @Override

    public Future<Void> writeCustomFrame(HttpFrame frame) {
        return delegate.writeCustomFrame(frame);
    }

    @Override

    public HttpServerResponse setStreamPriority(StreamPriority streamPriority) {
        delegate.setStreamPriority(streamPriority);
        return this;
    }

    @Override

    public HttpServerResponse addCookie(Cookie cookie) {
        delegate.addCookie(cookie);
        return this;
    }

    @Override
    public Cookie removeCookie(String name) {
        return delegate.removeCookie(name);
    }

    @Override

    public Cookie removeCookie(String name, boolean invalidate) {
        return delegate.removeCookie(name, invalidate);
    }

    @Override
    public Set<Cookie> removeCookies(String name) {
        return delegate.removeCookies(name);
    }

    @Override
    public Set<Cookie> removeCookies(String name, boolean invalidate) {
        return delegate.removeCookies(name, invalidate);
    }

    @Override
    public Cookie removeCookie(String name, String domain, String path) {
        return delegate.removeCookie(name, domain, path);
    }

    @Override
    public Cookie removeCookie(String name, String domain, String path, boolean invalidate) {
        return delegate.removeCookie(name, domain, path, invalidate);
    }

    @Override
    public boolean writeQueueFull() {
        return delegate.writeQueueFull();
    }
}
