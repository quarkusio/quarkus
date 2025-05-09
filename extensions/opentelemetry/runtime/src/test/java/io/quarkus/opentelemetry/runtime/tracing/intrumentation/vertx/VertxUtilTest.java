package io.quarkus.opentelemetry.runtime.tracing.intrumentation.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import org.junit.jupiter.api.Test;

import io.netty.handler.codec.DecoderResult;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;

class VertxUtilTest {

    @Test
    void extractRemoteHostnameTest() {
        assertEquals("localhost", VertxUtil.extractRemoteHostname(createDummyRequest("localhost:8080")));
        assertEquals("127.0.0.1", VertxUtil.extractRemoteHostname(createDummyRequest("127.0.0.1:111")));
        assertEquals("localhost", VertxUtil.extractRemoteHostname(createDummyRequest("localhost")));
        assertEquals("localhost", VertxUtil.extractRemoteHostname(createDummyRequest("localhost:")));
        assertEquals("", VertxUtil.extractRemoteHostname(createDummyRequest(":1111")));
        assertEquals("", VertxUtil.extractRemoteHostname(createDummyRequest(":")));
        assertEquals("", VertxUtil.extractRemoteHostname(createDummyRequest("")));
    }

    @Test
    void extractRemoteHostPortTest() {
        assertEquals(8080, VertxUtil.extractRemoteHostPort(createDummyRequest("localhost:8080")));
        assertEquals(111, VertxUtil.extractRemoteHostPort(createDummyRequest("127.0.0.1:111")));
        assertEquals(1010, VertxUtil.extractRemoteHostPort(createDummyRequest("localhost")));
        assertEquals(1010, VertxUtil.extractRemoteHostPort(createDummyRequest("localhost:")));
        assertEquals(1111, VertxUtil.extractRemoteHostPort(createDummyRequest(":1111")));
        assertEquals(1010, VertxUtil.extractRemoteHostPort(createDummyRequest(":")));
        assertEquals(1010, VertxUtil.extractRemoteHostPort(createDummyRequest("")));
    }

    private HttpServerRequest createDummyRequest(String hostHeader) {
        return new HttpServerRequest() {
            @Override
            public HttpServerRequest exceptionHandler(Handler<Throwable> handler) {
                return null;
            }

            @Override
            public HttpServerRequest handler(Handler<Buffer> handler) {
                return null;
            }

            @Override
            public HttpServerRequest pause() {
                return null;
            }

            @Override
            public HttpServerRequest resume() {
                return null;
            }

            @Override
            public HttpServerRequest fetch(long amount) {
                return null;
            }

            @Override
            public HttpServerRequest endHandler(Handler<Void> endHandler) {
                return null;
            }

            @Override
            public HttpVersion version() {
                return null;
            }

            @Override
            public HttpMethod method() {
                return null;
            }

            @Override
            public @Nullable String scheme() {
                return "";
            }

            @Override
            public String uri() {
                return "";
            }

            @Override
            public @Nullable String path() {
                return "";
            }

            @Override
            public @Nullable String query() {
                return "";
            }

            @Override
            public @Nullable HostAndPort authority() {
                return null;
            }

            @Override
            public @Nullable String host() {
                return "";
            }

            @Override
            public long bytesRead() {
                return 0;
            }

            @Override
            public HttpServerResponse response() {
                return null;
            }

            @Override
            public MultiMap headers() {
                HeadersMultiMap entries = new HeadersMultiMap();
                entries.add("host", hostHeader);
                return entries;
            }

            @Override
            public HttpServerRequest setParamsCharset(String charset) {
                return null;
            }

            @Override
            public String getParamsCharset() {
                return "";
            }

            @Override
            public MultiMap params(boolean semicolonIsNormalChar) {
                return null;
            }

            @Override
            public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
                return new X509Certificate[0];
            }

            @Override
            public String absoluteURI() {
                return "";
            }

            @Override
            public Future<Buffer> body() {
                return null;
            }

            @Override
            public Future<Void> end() {
                return null;
            }

            @Override
            public Future<NetSocket> toNetSocket() {
                return null;
            }

            @Override
            public HttpServerRequest setExpectMultipart(boolean expect) {
                return null;
            }

            @Override
            public boolean isExpectMultipart() {
                return false;
            }

            @Override
            public HttpServerRequest uploadHandler(@Nullable Handler<HttpServerFileUpload> uploadHandler) {
                return null;
            }

            @Override
            public MultiMap formAttributes() {
                return null;
            }

            @Override
            public @Nullable String getFormAttribute(String attributeName) {
                return "";
            }

            @Override
            public Future<ServerWebSocket> toWebSocket() {
                return null;
            }

            @Override
            public boolean isEnded() {
                return false;
            }

            @Override
            public HttpServerRequest customFrameHandler(Handler<HttpFrame> handler) {
                return null;
            }

            @Override
            public HttpConnection connection() {
                return null;
            }

            @Override
            public HttpServerRequest streamPriorityHandler(Handler<StreamPriority> handler) {
                return null;
            }

            @Override
            public DecoderResult decoderResult() {
                return null;
            }

            @Override
            public @Nullable Cookie getCookie(String name) {
                return null;
            }

            @Override
            public @Nullable Cookie getCookie(String name, String domain, String path) {
                return null;
            }

            @Override
            public Set<Cookie> cookies(String name) {
                return Set.of();
            }

            @Override
            public Set<Cookie> cookies() {
                return Set.of();
            }

            @Override
            public SocketAddress remoteAddress() {
                return SocketAddress.inetSocketAddress(1010, "localhost");
            }
        };
    }
}
