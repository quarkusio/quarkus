package io.quarkus.csrf.reactive.runtime;

import java.security.SecureRandom;
import java.util.Base64;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.WithFormRead;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.RoutingContext;

public class CsrfRequestResponseReactiveFilter {
    private static final Logger LOG = Logger.getLogger(CsrfRequestResponseReactiveFilter.class);

    /**
     * CSRF token key.
     */
    private static final String CSRF_TOKEN_KEY = "csrf_token";
    private static final String CSRF_TOKEN_BYTES_KEY = "csrf_token_bytes";

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
    @ServerRequestFilter
    @WithFormRead
    public void filter(ResteasyReactiveContainerRequestContext requestContext, RoutingContext routing) {
        final CsrfReactiveConfig config = this.configInstance.get();

        String cookieToken = getCookieToken(routing, config);
        if (cookieToken != null) {
            routing.put(CSRF_TOKEN_KEY, cookieToken);

            try {
                int cookieTokenSize = Base64.getUrlDecoder().decode(cookieToken).length;
                // HMAC SHA256 output is 32 bytes long
                int expectedCookieTokenSize = config.tokenSignatureKey.isPresent() ? 32 : config.tokenSize;
                if (cookieTokenSize != expectedCookieTokenSize) {
                    LOG.debugf("Invalid CSRF token cookie size: expected %d, got %d", expectedCookieTokenSize,
                            cookieTokenSize);
                    requestContext.abortWith(badClientRequest());
                    return;
                }
            } catch (IllegalArgumentException e) {
                LOG.debugf("Invalid CSRF token cookie: %s", cookieToken);
                requestContext.abortWith(badClientRequest());
                return;
            }
        }

        if (requestMethodIsSafe(requestContext)) {
            // safe HTTP method, tolerate the absence of a token
            if (cookieToken == null && isCsrfTokenRequired(routing, config)) {
                // Set the CSRF cookie with a randomly generated value
                byte[] tokenBytes = new byte[config.tokenSize];
                secureRandom.nextBytes(tokenBytes);
                routing.put(CSRF_TOKEN_BYTES_KEY, tokenBytes);
                routing.put(CSRF_TOKEN_KEY, Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes));
            }
        } else if (config.verifyToken) {
            // unsafe HTTP method, token is required

            if (!isMatchingMediaType(requestContext.getMediaType(), MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                    && !isMatchingMediaType(requestContext.getMediaType(), MediaType.MULTIPART_FORM_DATA_TYPE)) {
                if (config.requireFormUrlEncoded) {
                    LOG.debugf("Request has the wrong media type: %s", requestContext.getMediaType().toString());
                    requestContext.abortWith(badClientRequest());
                    return;
                } else {
                    LOG.debugf("Request has the  media type: %s, skipping the token verification",
                            requestContext.getMediaType().toString());
                    return;
                }
            }

            if (!requestContext.hasEntity()) {
                LOG.debug("Request has no entity");
                requestContext.abortWith(badClientRequest());
                return;
            }

            if (cookieToken == null) {
                LOG.debug("CSRF cookie is not found");
                requestContext.abortWith(badClientRequest());
                return;
            }

            ResteasyReactiveRequestContext rrContext = (ResteasyReactiveRequestContext) requestContext
                    .getServerRequestContext();
            String csrfToken = (String) rrContext.getFormParameter(config.formFieldName, true, false);
            if (csrfToken == null) {
                LOG.debug("CSRF token is not found");
                requestContext.abortWith(badClientRequest());
                return;
            } else {
                String expectedCookieTokenValue = config.tokenSignatureKey.isPresent()
                        ? CsrfTokenUtils.signCsrfToken(csrfToken, config.tokenSignatureKey.get())
                        : csrfToken;
                if (!cookieToken.equals(expectedCookieTokenValue)) {
                    LOG.debug("CSRF token value is wrong");
                    requestContext.abortWith(badClientRequest());
                    return;
                } else {
                    routing.put(CSRF_TOKEN_VERIFIED, true);
                    return;
                }
            }
        } else if (cookieToken == null) {
            LOG.debug("CSRF token is not found");
            requestContext.abortWith(badClientRequest());
        }
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

            String cookieValue = null;
            if (config.tokenSignatureKey.isPresent()) {
                byte[] csrfTokenBytes = (byte[]) routing.get(CSRF_TOKEN_BYTES_KEY);

                if (csrfTokenBytes == null) {
                    LOG.debug("CSRF Request Filter did not set the property " + CSRF_TOKEN_BYTES_KEY
                            + ", no CSRF cookie will be created");
                    return;
                }
                cookieValue = CsrfTokenUtils.signCsrfToken(csrfTokenBytes, config.tokenSignatureKey.get());
            } else {
                String csrfToken = (String) routing.get(CSRF_TOKEN_KEY);

                if (csrfToken == null) {
                    LOG.debug("CSRF Request Filter did not set the property " + CSRF_TOKEN_KEY
                            + ", no CSRF cookie will be created");
                    return;
                }
                cookieValue = csrfToken;
            }

            createCookie(cookieValue, routing, config);
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
}
