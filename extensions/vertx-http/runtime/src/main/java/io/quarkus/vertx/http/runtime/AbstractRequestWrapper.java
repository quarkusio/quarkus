package io.quarkus.vertx.http.runtime;

import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import io.netty.handler.codec.DecoderResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpFrame;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.StreamPriority;
import io.vertx.core.http.impl.HttpServerRequestInternal;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

public abstract class AbstractRequestWrapper
        implements HttpServerRequest, HttpServerRequestInternal {

    protected final HttpServerRequestInternal delegate;

    protected AbstractRequestWrapper(HttpServerRequest request) {
        delegate = (HttpServerRequestInternal) request;
    }

    @Override
    public long bytesRead() {
        return delegate.bytesRead();
    }

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        delegate.exceptionHandler(handler);
        return this;
    }

    @Override
    public HttpServerRequest handler(Handler<Buffer> handler) {
        delegate.handler(handler);
        return this;
    }

    @Override
    public HttpServerRequest pause() {
        delegate.pause();
        return this;
    }

    @Override
    public HttpServerRequest resume() {
        delegate.resume();
        return this;
    }

    @Override
    public HttpServerRequest fetch(long amount) {
        delegate.fetch(amount);
        return this;
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> handler) {
        delegate.endHandler(handler);
        return this;
    }

    @Override
    public HttpVersion version() {
        return delegate.version();
    }

    @Override
    public HttpMethod method() {
        return delegate.method();
    }

    @Override
    public String uri() {
        return delegate.uri();
    }

    @Override
    public String path() {
        return delegate.path();
    }

    @Override
    public String query() {
        return delegate.query();
    }

    @Override
    public HttpServerResponse response() {
        return delegate.response();
    }

    @Override
    public MultiMap headers() {
        return delegate.headers();
    }

    @Override
    public String getHeader(String s) {
        return delegate.getHeader(s);
    }

    @Override
    public String getHeader(CharSequence charSequence) {
        return delegate.getHeader(charSequence);
    }

    @Override
    public MultiMap params() {
        return delegate.params();
    }

    @Override
    public String getParam(String s) {
        return delegate.getParam(s);
    }

    @Override
    public SocketAddress remoteAddress() {
        return delegate.remoteAddress();
    }

    @Override
    public SocketAddress localAddress() {
        return delegate.localAddress();
    }

    @Override
    public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
        return delegate.peerCertificateChain();
    }

    @Override
    public SSLSession sslSession() {
        return delegate.sslSession();
    }

    @Override
    public String absoluteURI() {
        return delegate.absoluteURI();
    }

    @Override
    public String scheme() {
        return delegate.scheme();
    }

    @Override
    public String host() {
        return delegate.host();
    }

    @Override
    public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
        delegate.customFrameHandler(handler);
        return this;
    }

    @Override
    public HttpConnection connection() {
        return delegate.connection();
    }

    @Override
    public HttpServerRequest bodyHandler(Handler<Buffer> handler) {
        delegate.bodyHandler(handler);
        return this;
    }

    @Override
    public HttpServerRequest setExpectMultipart(boolean b) {
        delegate.setExpectMultipart(b);
        return this;
    }

    @Override
    public boolean isExpectMultipart() {
        return delegate.isExpectMultipart();
    }

    @Override
    public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> handler) {
        delegate.uploadHandler(handler);
        return this;
    }

    @Override
    public MultiMap formAttributes() {
        return delegate.formAttributes();
    }

    @Override
    public String getFormAttribute(String s) {
        return delegate.getFormAttribute(s);
    }

    @Override
    public boolean isEnded() {
        return delegate.isEnded();
    }

    @Override
    public boolean isSSL() {
        return delegate.isSSL();
    }

    @Override
    public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> handler) {
        delegate.streamPriorityHandler(handler);
        return this;
    }

    @Override
    public StreamPriority streamPriority() {
        return delegate.streamPriority();
    }

    @Override
    public Cookie getCookie(String name) {
        return delegate.getCookie(name);
    }

    @Override
    public int cookieCount() {
        return delegate.cookieCount();
    }

    @Override
    public Map<String, Cookie> cookieMap() {
        return delegate.cookieMap();
    }

    @Override
    public HttpServerRequest body(Handler<AsyncResult<Buffer>> handler) {
        return delegate.body(handler);
    }

    @Override
    public void end(Handler<AsyncResult<Void>> handler) {
        delegate.end(handler);
    }

    @Override
    public void toNetSocket(Handler<AsyncResult<NetSocket>> handler) {
        delegate.toNetSocket(handler);
    }

    @Override
    public void toWebSocket(Handler<AsyncResult<ServerWebSocket>> handler) {
        delegate.toWebSocket(handler);
    }

    @Override
    public HttpServerRequest routed(String route) {
        return delegate.routed(route);
    }

    @Override
    public Future<Buffer> body() {
        return delegate.body();
    }

    @Override
    public Future<Void> end() {
        return delegate.end();
    }

    @Override
    public Future<NetSocket> toNetSocket() {
        return delegate.toNetSocket();
    }

    @Override
    public Future<ServerWebSocket> toWebSocket() {
        return delegate.toWebSocket();
    }

    @Override
    public Context context() {
        return delegate.context();
    }

    @Override
    public Object metric() {
        return delegate.metric();
    }

    @Override
    public DecoderResult decoderResult() {
        return delegate.decoderResult();
    }
}
