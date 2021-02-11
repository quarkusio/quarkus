package io.quarkus.vertx.http.runtime.filters;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.StreamPriority;

class AbstractResponseWrapper implements HttpServerResponse {

    private final HttpServerResponse delegate;

    AbstractResponseWrapper(HttpServerResponse delegate) {
        this.delegate = delegate;
    }

    @Override
    public HttpServerResponse exceptionHandler(Handler<Throwable> handler) {
        delegate.exceptionHandler(handler);
        return this;
    }

    @Override
    public HttpServerResponse write(Buffer data) {
        delegate.write(data);
        return this;
    }

    @Override
    public HttpServerResponse write(Buffer data, Handler<AsyncResult<Void>> handler) {
        delegate.write(data, handler);
        return this;
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

    public HttpServerResponse write(String chunk, String enc) {
        delegate.write(chunk, enc);
        return this;
    }

    @Override

    public HttpServerResponse write(String chunk, String enc, Handler<AsyncResult<Void>> handler) {
        delegate.write(chunk, enc, handler);
        return this;
    }

    @Override

    public HttpServerResponse write(String chunk) {
        delegate.write(chunk);
        return this;
    }

    @Override

    public HttpServerResponse write(String chunk, Handler<AsyncResult<Void>> handler) {
        delegate.write(chunk, handler);
        return this;
    }

    @Override

    public HttpServerResponse writeContinue() {
        delegate.writeContinue();
        return this;
    }

    @Override
    public void end(String chunk) {
        delegate.end(chunk);
    }

    @Override
    public void end(String chunk, Handler<AsyncResult<Void>> handler) {
        delegate.end(chunk, handler);
    }

    @Override
    public void end(String chunk, String enc) {
        delegate.end(chunk, enc);
    }

    @Override
    public void end(String chunk, String enc, Handler<AsyncResult<Void>> handler) {
        delegate.end(chunk, enc, handler);
    }

    @Override
    public void end(Buffer chunk) {
        delegate.end(chunk);
    }

    @Override
    public void end(Buffer chunk, Handler<AsyncResult<Void>> handler) {
        delegate.end(chunk, handler);
    }

    @Override
    public void end() {
        delegate.end();
    }

    @Override

    public HttpServerResponse sendFile(String filename) {
        delegate.sendFile(filename);
        return this;
    }

    @Override

    public HttpServerResponse sendFile(String filename, long offset) {
        delegate.sendFile(filename, offset);
        return this;
    }

    @Override

    public HttpServerResponse sendFile(String filename, long offset, long length) {
        delegate.sendFile(filename, offset, length);
        return this;
    }

    @Override

    public HttpServerResponse sendFile(String filename, Handler<AsyncResult<Void>> resultHandler) {
        delegate.sendFile(filename, resultHandler);
        return this;
    }

    @Override

    public HttpServerResponse sendFile(String filename, long offset, Handler<AsyncResult<Void>> resultHandler) {
        delegate.sendFile(filename, offset, resultHandler);
        return this;
    }

    @Override

    public HttpServerResponse sendFile(String filename, long offset, long length,
            Handler<AsyncResult<Void>> resultHandler) {
        delegate.sendFile(filename, offset, length, resultHandler);
        return this;
    }

    @Override
    public void close() {
        delegate.close();
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

    public HttpServerResponse push(HttpMethod method, String host, String path,
            Handler<AsyncResult<HttpServerResponse>> handler) {
        delegate.push(method, host, path, handler);
        return this;
    }

    @Override

    public HttpServerResponse push(HttpMethod method, String path, MultiMap headers,
            Handler<AsyncResult<HttpServerResponse>> handler) {
        delegate.push(method, path, headers, handler);
        return this;
    }

    @Override

    public HttpServerResponse push(HttpMethod method, String path, Handler<AsyncResult<HttpServerResponse>> handler) {
        delegate.push(method, path, handler);
        return this;
    }

    @Override

    public HttpServerResponse push(HttpMethod method, String host, String path, MultiMap headers,
            Handler<AsyncResult<HttpServerResponse>> handler) {
        delegate.push(method, host, path, headers, handler);
        return this;
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public void reset(long code) {
        delegate.reset(code);
    }

    @Override

    public HttpServerResponse writeCustomFrame(int type, int flags, Buffer payload) {
        delegate.writeCustomFrame(type, flags, payload);
        return this;
    }

    @Override

    public HttpServerResponse writeCustomFrame(HttpFrame frame) {
        delegate.writeCustomFrame(frame);
        return this;
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
    public void end(Handler<AsyncResult<Void>> handler) {
        delegate.end(handler);
    }

    @Override
    public boolean writeQueueFull() {
        return delegate.writeQueueFull();
    }
}
