package io.quarkus.vertx.http.runtime;

import java.util.Map;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

class ForwardedServerRequestWrapper implements HttpServerRequest {

    private final HttpServerRequest delegate;
    private final ForwardedParser forwardedParser;

    private boolean modified;

    private HttpMethod method;
    private String path;
    private String query;
    private String uri;
    private String absoluteURI;

    ForwardedServerRequestWrapper(HttpServerRequest request, boolean allowForwarded) {
        delegate = request;
        forwardedParser = new ForwardedParser(delegate, allowForwarded);
    }

    void changeTo(HttpMethod method, String uri) {
        modified = true;
        this.method = method;
        this.uri = uri;
        // lazy initialization
        this.path = null;
        this.query = null;
        this.absoluteURI = null;

        // parse
        int queryIndex = uri.indexOf('?');
        int fragmentIndex = uri.indexOf('#');

        // there's a query
        if (queryIndex != -1) {
            path = uri.substring(0, queryIndex);
            // there's a fragment
            if (fragmentIndex != -1) {
                query = uri.substring(queryIndex + 1, fragmentIndex);
            } else {
                query = uri.substring(queryIndex + 1);
            }
        } else {
            // there's a fragment
            if (fragmentIndex != -1) {
                path = uri.substring(0, fragmentIndex);
            } else {
                path = uri;
            }
        }
    }

    @Override
    public long bytesRead() {
        return delegate.bytesRead();
    }

    @Override
    public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
        return delegate.exceptionHandler(handler);
    }

    @Override
    public HttpServerRequest handler(Handler<Buffer> handler) {
        return delegate.handler(handler);
    }

    @Override
    public HttpServerRequest pause() {
        return delegate.pause();
    }

    @Override
    public HttpServerRequest resume() {
        return delegate.resume();
    }

    @Override
    public HttpServerRequest fetch(long amount) {
        return delegate.fetch(amount);
    }

    @Override
    public HttpServerRequest endHandler(Handler<Void> handler) {
        return delegate.endHandler(handler);
    }

    @Override
    public HttpVersion version() {
        return delegate.version();
    }

    @Override
    public HttpMethod method() {
        if (!modified) {
            return delegate.method();
        }
        return method;
    }

    @Override
    public String rawMethod() {
        if (!modified) {
            return delegate.rawMethod();
        }
        return method.toString();
    }

    @Override
    public String uri() {
        if (!modified) {
            return delegate.uri();
        }
        return uri;
    }

    @Override
    public String path() {
        if (!modified) {
            return delegate.path();
        }
        return path;
    }

    @Override
    public String query() {
        if (!modified) {
            return delegate.query();
        }
        return query;
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
        return forwardedParser.remoteAddress();
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
        if (!modified) {
            return forwardedParser.absoluteURI();
        } else {
            if (absoluteURI == null) {
                String scheme = forwardedParser.scheme();
                String host = forwardedParser.host();

                // if both are not null we can rebuild the uri
                if (scheme != null && host != null) {
                    absoluteURI = scheme + "://" + host + uri;
                } else {
                    absoluteURI = uri;
                }
            }

            return absoluteURI;
        }
    }

    @Override
    public String scheme() {
        return forwardedParser.scheme();
    }

    @Override
    public String host() {
        return forwardedParser.host();
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
        return delegate.bodyHandler(handler);
    }

    @Override
    public NetSocket netSocket() {
        return delegate.netSocket();
    }

    @Override
    public HttpServerRequest setExpectMultipart(boolean b) {
        return delegate.setExpectMultipart(b);
    }

    @Override
    public boolean isExpectMultipart() {
        return delegate.isExpectMultipart();
    }

    @Override
    public HttpServerRequest uploadHandler(Handler<HttpServerFileUpload> handler) {
        return delegate.uploadHandler(handler);
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
        return forwardedParser.isSSL();
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
