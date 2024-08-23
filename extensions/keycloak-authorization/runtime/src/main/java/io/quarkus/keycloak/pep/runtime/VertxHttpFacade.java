package io.quarkus.keycloak.pep.runtime;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.keycloak.adapters.authorization.TokenPrincipal;
import org.keycloak.adapters.authorization.spi.HttpRequest;
import org.keycloak.adapters.authorization.spi.HttpResponse;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.vertx.http.runtime.VertxInputStream;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class VertxHttpFacade implements HttpRequest, HttpResponse {

    private final long readTimeout;
    private final HttpRequest request;
    private final HttpResponse response;
    private final TokenPrincipal tokenPrincipal;

    public VertxHttpFacade(RoutingContext routingContext, String token, long readTimeout) {
        this.readTimeout = readTimeout;
        this.request = createRequest(routingContext);
        this.response = createResponse(routingContext);
        tokenPrincipal = new TokenPrincipal() {
            @Override
            public String getRawToken() {
                return token;
            }
        };
    }

    @Override
    public String getRelativePath() {
        return request.getRelativePath();
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getURI() {
        return request.getURI();
    }

    @Override
    public List<String> getHeaders(String name) {
        return request.getHeaders(name);
    }

    @Override
    public String getFirstParam(String name) {
        return request.getFirstParam(name);
    }

    @Override
    public String getCookieValue(String name) {
        return request.getCookieValue(name);
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }

    @Override
    public boolean isSecure() {
        return request.isSecure();
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public InputStream getInputStream(boolean buffered) {
        return request.getInputStream(buffered);
    }

    @Override
    public TokenPrincipal getPrincipal() {
        return request.getPrincipal();
    }

    @Override
    public void sendError(int statusCode) {
        response.sendError(statusCode);
    }

    @Override
    public void sendError(int statusCode, String reason) {
        response.sendError(statusCode, reason);
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    private HttpRequest createRequest(RoutingContext routingContext) {
        HttpServerRequest request = routingContext.request();
        return new HttpRequest() {
            @Override
            public String getMethod() {
                return request.method().name();
            }

            @Override
            public String getURI() {
                return request.absoluteURI();
            }

            @Override
            public String getRelativePath() {
                return routingContext.normalizedPath();
            }

            @Override
            public boolean isSecure() {
                return request.isSSL();
            }

            @Override
            public String getFirstParam(String param) {
                return request.getParam(param);
            }

            @Override
            public String getCookieValue(String name) {
                Cookie cookie = request.getCookie(name);

                if (cookie == null) {
                    return null;
                }

                return cookie.getValue();
            }

            @Override
            public String getHeader(String name) {
                return request.getHeader(name);
            }

            @Override
            public List<String> getHeaders(String name) {
                return request.headers().getAll(name);
            }

            @Override
            public InputStream getInputStream(boolean buffered) {
                try {
                    if (routingContext.getBody() != null) {
                        return new ByteArrayInputStream(routingContext.getBody().getBytes());
                    }
                    if (routingContext.request().isEnded()) {
                        return new ByteArrayInputStream(new byte[0]);
                    }
                    return new VertxInputStream(routingContext, readTimeout);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TokenPrincipal getPrincipal() {
                return tokenPrincipal;
            }

            @Override
            public String getRemoteAddr() {
                return request.remoteAddress().host();
            }
        };
    }

    private HttpResponse createResponse(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        return new HttpResponse() {

            @Override
            public void setHeader(String name, String value) {
                response.headers().set(name, value);
            }

            @Override
            public void sendError(int code) {
                response.setStatusCode(code);
            }

            @Override
            public void sendError(int code, String message) {
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
                response.setStatusCode(code);
                response.setStatusMessage(message);
            }
        };
    }
}
