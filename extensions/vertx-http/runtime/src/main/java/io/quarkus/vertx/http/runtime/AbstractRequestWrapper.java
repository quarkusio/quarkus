package io.quarkus.vertx.http.runtime;

import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

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
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

public abstract class AbstractRequestWrapper implements HttpServerRequest {

    protected final HttpServerRequest delegate;

    protected AbstractRequestWrapper(HttpServerRequest request) {
        delegate = request;
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
    public String rawMethod() {
        return delegate.rawMethod();
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
    public NetSocket netSocket() {
        return delegate.netSocket();
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
    public ServerWebSocket upgrade() {
        return delegate.upgrade();
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

}
