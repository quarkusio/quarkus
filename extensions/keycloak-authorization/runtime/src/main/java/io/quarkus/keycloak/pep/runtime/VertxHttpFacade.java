package io.quarkus.keycloak.pep.runtime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.cert.X509Certificate;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.LogoutError;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.vertx.http.runtime.VertxInputStream;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.ext.web.RoutingContext;

public class VertxHttpFacade implements OIDCHttpFacade {

    private final Response response;
    private final RoutingContext routingContext;
    private final Request request;
    private final String token;
    private final long readTimeout;

    public VertxHttpFacade(RoutingContext routingContext, String token, long readTimeout) {
        this.routingContext = routingContext;
        this.token = token;
        this.readTimeout = readTimeout;
        this.request = createRequest(routingContext);
        this.response = createResponse(routingContext);
    }

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        try {
            return routingContext.request().peerCertificateChain();
        } catch (SSLPeerUnverifiedException e) {
            throw new RuntimeException("Failed to fetch certificates from request", e);
        }
    }

    private Request createRequest(RoutingContext routingContext) {
        HttpServerRequest request = routingContext.request();
        return new Request() {
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
                return URI.create(request.uri()).getPath();
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
            public String getQueryParamValue(String param) {
                return request.getParam(param);
            }

            @Override
            public Cookie getCookie(String cookieName) {
                io.vertx.core.http.Cookie c = request.getCookie(cookieName);

                if (c == null) {
                    return null;
                }

                return new Cookie(c.getName(), c.getValue(), 1, c.getDomain(), c.getPath());
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
            public InputStream getInputStream() {
                return getInputStream(false);
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
            public String getRemoteAddr() {
                return request.remoteAddress().host();
            }

            @Override
            public void setError(AuthenticationError error) {
                // no-op
            }

            @Override
            public void setError(LogoutError error) {
                // no-op
            }
        };
    }

    private Response createResponse(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();

        return new Response() {
            @Override
            public void setStatus(int status) {
                response.setStatusCode(status);
            }

            @Override
            public void addHeader(String name, String value) {
                response.headers().add(name, value);
            }

            @Override
            public void setHeader(String name, String value) {
                response.headers().set(name, value);
            }

            @Override
            public void resetCookie(String name, String path) {
                response.removeCookie(name, true);
            }

            @Override
            public void setCookie(String name, String value, String path, String domain, int maxAge, boolean secure,
                    boolean httpOnly) {
                CookieImpl cookie = new CookieImpl(name, value);

                cookie.setPath(path);
                cookie.setDomain(domain);
                cookie.setMaxAge(maxAge);
                cookie.setSecure(secure);
                cookie.setHttpOnly(httpOnly);

                response.addCookie(cookie);
            }

            @Override
            public OutputStream getOutputStream() {
                ByteArrayOutputStream os = new ByteArrayOutputStream();

                response.headersEndHandler(event -> response.write(Buffer.buffer().appendBytes(os.toByteArray())));

                return os;
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

            @Override
            public void end() {
                response.end();
            }
        };
    }

    @Override
    public KeycloakSecurityContext getSecurityContext() {
        try {
            return new KeycloakSecurityContext(token, new JWSInput(token).readJsonContent(AccessToken.class), null, null);
        } catch (JWSInputException e) {
            throw new RuntimeException("Failed to create access token", e);
        }
    }
}
