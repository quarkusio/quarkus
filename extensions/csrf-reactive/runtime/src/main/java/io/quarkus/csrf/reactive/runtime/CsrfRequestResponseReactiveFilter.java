package io.quarkus.csrf.reactive.runtime;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.RoutingContext;

public class CsrfRequestResponseReactiveFilter {
    private static final Logger LOG = Logger.getLogger(CsrfRequestResponseReactiveFilter.class);

    /**
     * CSRF token key.
     */
    private static final String CSRF_TOKEN_KEY = "csrf_token";

    /**
     * CSRF token verification status.
     */
    private static final String CSRF_TOKEN_VERIFIED = "csrf_token_verified";

    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    Instance<CsrfReactiveConfig> configInstance;

    public CsrfRequestResponseReactiveFilter() {
    }

    /**
     * If the request method is safe ({@code GET}, {@code HEAD} or {@code OPTIONS}):
     * <ul>
     * <li>Sets a {@link RoutingContext} key by the name {@value #CSRF_TOKEN_KEY} that contains a randomly generated Base64
     * encoded string, unless such a cookie was already sent in the incoming request.</li>
     * </ul>
     * If the request method is unsafe, requires the following:
     * <ul>
     * <li>The request contains a valid CSRF token cookie set in response to a previous request (see above).</li>
     * <li>A request entity is present.</li>
     * <li>The request {@code Content-Type} is {@value MediaType#APPLICATION_FORM_URLENCODED}.</li>
     * <li>The request entity contains a form parameter with the name
     * {@value #CSRF_TOKEN_KEY} and value that is equal to the one supplied in the cookie.</li>
     * </ul>
     */
    @ServerRequestFilter(preMatching = true)
    public Uni<Response> filter(ContainerRequestContext requestContext, RoutingContext routing) {
        final CsrfReactiveConfig config = this.configInstance.get();

        String cookieToken = getCookieToken(routing, config);
        if (cookieToken != null) {
            routing.put(CSRF_TOKEN_KEY, cookieToken);

            try {
                int suppliedTokenSize = Base64.getUrlDecoder().decode(cookieToken).length;

                if (suppliedTokenSize != config.tokenSize) {
                    LOG.debugf("Invalid CSRF token cookie size: expected %d, got %d", config.tokenSize,
                            suppliedTokenSize);
                    return Uni.createFrom().item(badClientRequest());
                }
            } catch (IllegalArgumentException e) {
                LOG.debugf("Invalid CSRF token cookie: %s", cookieToken);
                return Uni.createFrom().item(badClientRequest());
            }
        }

        if (requestMethodIsSafe(requestContext)) {
            // safe HTTP method, tolerate the absence of a token
            if (cookieToken == null && isCsrfTokenRequired(routing, config)) {
                // Set the CSRF cookie with a randomly generated value
                byte[] token = new byte[config.tokenSize];
                secureRandom.nextBytes(token);
                routing.put(CSRF_TOKEN_KEY, Base64.getUrlEncoder().withoutPadding().encodeToString(token));
            }
        } else if (config.verifyToken) {
            // unsafe HTTP method, token is required

            if (!isMatchingMediaType(requestContext.getMediaType(), MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                    && !isMatchingMediaType(requestContext.getMediaType(), MediaType.MULTIPART_FORM_DATA_TYPE)) {
                if (config.requireFormUrlEncoded) {
                    LOG.debugf("Request has the wrong media type: %s", requestContext.getMediaType().toString());
                    return Uni.createFrom().item(badClientRequest());
                } else {
                    LOG.debugf("Request has the  media type: %s, skipping the token verification",
                            requestContext.getMediaType().toString());
                    return Uni.createFrom().nullItem();
                }
            }

            if (!requestContext.hasEntity()) {
                LOG.debug("Request has no entity");
                return Uni.createFrom().item(badClientRequest());
            }

            if (cookieToken == null) {
                LOG.debug("CSRF cookie is not found");
                return Uni.createFrom().item(badClientRequest());
            }

            return getFormUrlEncodedData(routing.request())
                    .flatMap(new Function<MultiMap, Uni<? extends Response>>() {
                        @Override
                        public Uni<Response> apply(MultiMap form) {

                            String csrfToken = form.get(config.formFieldName);
                            if (csrfToken == null) {
                                LOG.debug("CSRF token is not found");
                                return Uni.createFrom().item(badClientRequest());
                            } else if (!csrfToken.equals(cookieToken)) {
                                LOG.debug("CSRF token value is wrong");
                                return Uni.createFrom().item(badClientRequest());
                            } else {
                                routing.put(CSRF_TOKEN_VERIFIED, true);
                            }
                            return Uni.createFrom().nullItem();
                        }
                    });
        } else if (cookieToken == null) {
            LOG.debug("CSRF token is not found");
            return Uni.createFrom().item(badClientRequest());
        }

        return null;
    }

    private static boolean isMatchingMediaType(MediaType contentType, MediaType expectedType) {
        return contentType.getType().equals(expectedType.getType())
                && contentType.getSubtype().equals(expectedType.getSubtype());
    }

    private static Response badClientRequest() {
        return Response.status(400).build();
    }

    /**
     * If the requirements below are true, sets a cookie by the name {@value #CSRF_TOKEN_KEY} that contains a CSRF token.
     * <ul>
     * <li>The request method is {@code GET}.</li>
     * <li>The request does not contain a valid CSRF token cookie.</li>
     * </ul>
     *
     * @throws IllegalStateException if the {@link RoutingContext} does not have a value for the key {@value #CSRF_TOKEN_KEY}
     *         and a cookie needs to be set.
     */
    @ServerResponseFilter
    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext, RoutingContext routing) {
        final CsrfReactiveConfig config = configInstance.get();
        if (requestContext.getMethod().equals("GET") && isCsrfTokenRequired(routing, config)
                && getCookieToken(routing, config) == null) {
            String token = (String) routing.get(CSRF_TOKEN_KEY);

            if (token == null) {
                throw new IllegalStateException(
                        "CSRF Filter should have set the property " + CSRF_TOKEN_KEY + ", but it is null");
            }

            createCookie(token, routing, config);
        }

    }

    /**
     * Gets the CSRF token from the CSRF cookie from the current {@code RoutingContext}.
     *
     * @return An Optional containing the token, or an empty Optional if the token cookie is not present or is invalid
     */
    private String getCookieToken(RoutingContext routing, CsrfReactiveConfig config) {
        Cookie cookie = routing.getCookie(config.cookieName);

        if (cookie == null) {
            LOG.debug("CSRF token cookie is not set");
            return null;
        }

        return cookie.getValue();
    }

    private boolean isCsrfTokenRequired(RoutingContext routing, CsrfReactiveConfig config) {
        return config.createTokenPath.isPresent() ? config.createTokenPath.get().contains(routing.request().path()) : true;
    }

    private void createCookie(String csrfToken, RoutingContext routing, CsrfReactiveConfig config) {
        ServerCookie cookie = new CookieImpl(config.cookieName, csrfToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(config.cookieForceSecure || routing.request().isSSL());
        cookie.setMaxAge(config.cookieMaxAge.toSeconds());
        cookie.setPath(config.cookiePath);
        if (config.cookieDomain.isPresent()) {
            cookie.setDomain(config.cookieDomain.get());
        }
        routing.response().addCookie(cookie);
    }

    private static boolean requestMethodIsSafe(ContainerRequestContext context) {
        switch (context.getMethod()) {
            case "GET":
            case "HEAD":
            case "OPTIONS":
                return true;
            default:
                return false;
        }
    }

    private static Uni<MultiMap> getFormUrlEncodedData(HttpServerRequest request) {
        request.setExpectMultipart(true);
        return Uni.createFrom().emitter(new Consumer<UniEmitter<? super MultiMap>>() {
            @Override
            public void accept(UniEmitter<? super MultiMap> t) {
                request.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        t.complete(request.formAttributes());
                    }
                });
                request.resume();
            }
        });
    }

}
