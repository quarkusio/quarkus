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
    private static final String NEW_COOKIE_REQUIRED = "true";

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
            if (isCsrfTokenRequired(routing, config)) {
                if (cookieToken == null) {
                    generateNewCsrfToken(routing, config);
                } else {
                    String csrfTokenHeaderParam = requestContext.getHeaderString(config.tokenHeaderName);
                    if (csrfTokenHeaderParam != null) {
                        LOG.debugf("CSRF token found in the token header");
                        // Verify the header, make sure the header value, possibly signed, is returned as the next cookie value
                        verifyCsrfToken(requestContext, routing, config, cookieToken, csrfTokenHeaderParam);
                    } else if (!config.tokenSignatureKey.isEmpty()) {
                        // If the signature is required, then we can not use the current cookie value
                        // as the HTML form token key because it represents a signed value of the previous key
                        // and it will lead to the double-signing issue if this value is reused as the key.
                        // It should be fine for simple HTML forms anyway
                        generateNewCsrfToken(routing, config);
                    } else {
                        // Make sure the same cookie value is returned
                        routing.put(CSRF_TOKEN_KEY, cookieToken);
                        routing.put(CSRF_TOKEN_BYTES_KEY, Base64.getUrlDecoder().decode(cookieToken));
                    }
                }
                routing.put(NEW_COOKIE_REQUIRED, true);
            }
        } else if (config.verifyToken) {
            // unsafe HTTP method, token is required

            // Check the header first
            String csrfTokenHeaderParam = requestContext.getHeaderString(config.tokenHeaderName);
            if (csrfTokenHeaderParam != null) {
                LOG.debugf("CSRF token found in the token header");
                verifyCsrfToken(requestContext, routing, config, cookieToken, csrfTokenHeaderParam);
                return;
            }

            // Check the form field
            MediaType mediaType = requestContext.getMediaType();
            if (!isMatchingMediaType(mediaType, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                    && !isMatchingMediaType(mediaType, MediaType.MULTIPART_FORM_DATA_TYPE)) {
                if (config.requireFormUrlEncoded) {
                    LOG.debugf("Request has the wrong media type: %s", mediaType);
                    requestContext.abortWith(badClientRequest());
                    return;
                } else {
                    LOG.debugf("Request has the media type: %s, skipping the token verification",
                            mediaType);
                    return;
                }
            }

            if (!requestContext.hasEntity()) {
                LOG.debug("Request has no entity");
                requestContext.abortWith(badClientRequest());
                return;
            }

            ResteasyReactiveRequestContext rrContext = (ResteasyReactiveRequestContext) requestContext
                    .getServerRequestContext();
            String csrfTokenFormParam = (String) rrContext.getFormParameter(config.formFieldName, true, false);
            LOG.debugf("CSRF token found in the form parameter");
            verifyCsrfToken(requestContext, routing, config, cookieToken, csrfTokenFormParam);

        } else if (cookieToken == null) {
            LOG.debug("CSRF token is not found");
            requestContext.abortWith(badClientRequest());
        }
    }

    private void generateNewCsrfToken(RoutingContext routing, CsrfReactiveConfig config) {
        // Set the CSRF cookie with a randomly generated value
        byte[] tokenBytes = new byte[config.tokenSize];
        secureRandom.nextBytes(tokenBytes);
        routing.put(CSRF_TOKEN_BYTES_KEY, tokenBytes);
        routing.put(CSRF_TOKEN_KEY, Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes));
    }

    private void verifyCsrfToken(ResteasyReactiveContainerRequestContext requestContext, RoutingContext routing,
            CsrfReactiveConfig config, String cookieToken, String csrfToken) {
        if (cookieToken == null) {
            LOG.debug("CSRF cookie is not found");
            requestContext.abortWith(badClientRequest());
            return;
        }
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
                routing.put(CSRF_TOKEN_KEY, csrfToken);
                routing.put(CSRF_TOKEN_BYTES_KEY, Base64.getUrlDecoder().decode(csrfToken));
                routing.put(CSRF_TOKEN_VERIFIED, true);
                // reset the cookie
                routing.put(NEW_COOKIE_REQUIRED, true);
                return;
            }
        }
    }

    /**
     * Compares if {@link MediaType} matches the expected type.
     * <p>
     * Note: isCompatible is taking wildcards, which is why we individually compare types and subtypes,
     * so if someone sends a <code>Content-Type: *</code> it will be marked as compatible which is a problem
     */
    private static boolean isMatchingMediaType(MediaType contentType, MediaType expectedType) {
        if (contentType == null) {
            return (expectedType == null);
        }
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
        if (routing.get(NEW_COOKIE_REQUIRED) != null) {

            final CsrfReactiveConfig config = configInstance.get();

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
    private static String getCookieToken(RoutingContext routing, CsrfReactiveConfig config) {
        Cookie cookie = routing.getCookie(config.cookieName);

        if (cookie == null) {
            LOG.debug("CSRF token cookie is not set");
            return null;
        }

        return cookie.getValue();
    }

    private static boolean isCsrfTokenRequired(RoutingContext routing, CsrfReactiveConfig config) {
        return config.createTokenPath
                .map(value -> value.contains(routing.normalizedPath())).orElse(true);
    }

    private static void createCookie(String cookieTokenValue, RoutingContext routing, CsrfReactiveConfig config) {

        ServerCookie cookie = new CookieImpl(config.cookieName, cookieTokenValue);
        cookie.setHttpOnly(config.cookieHttpOnly);
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
