package io.quarkus.csrf.reactive.runtime;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.security.SecureRandom;
import java.util.Base64;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.GenericRuntimeConfigurableServerRestHandler;

import io.vertx.core.http.Cookie;
import io.vertx.ext.web.RoutingContext;

public class CsrfHandler implements GenericRuntimeConfigurableServerRestHandler<CsrfReactiveConfig> {
    private static final Logger LOG = Logger.getLogger(CsrfHandler.class);

    /**
     * CSRF token key.
     */
    private static final String CSRF_TOKEN_KEY = "csrf_token";
    private static final String CSRF_TOKEN_BYTES_KEY = "csrf_token_bytes";

    /**
     * CSRF token verification status.
     */
    private static final String CSRF_TOKEN_VERIFIED = "csrf_token_verified";

    // although technically the field does not need to be volatile (since the access mode is determined by the VarHandle use)
    // it is a recommended practice by Doug Lea meant to catch cases where the field is accessed directly (by accident)
    @SuppressWarnings("unused")
    private volatile SecureRandom secureRandom;

    // use a VarHandle to access the secureRandom as the value is written only by the main thread
    // and all other threads simply read the value, and thus we can use the Release / Acquire access mode
    private static final VarHandle SECURE_RANDOM_VH;

    static {
        try {
            SECURE_RANDOM_VH = MethodHandles.lookup().findVarHandle(CsrfHandler.class, "secureRandom",
                    SecureRandom.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    private CsrfReactiveConfig config;

    public CsrfHandler() {
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
    public void handle(ResteasyReactiveRequestContext reactiveRequestContext) {
        final ContainerRequestContext requestContext = reactiveRequestContext.getContainerRequestContext();

        final RoutingContext routing = reactiveRequestContext.serverRequest().unwrap(RoutingContext.class);

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
                getSecureRandom().nextBytes(tokenBytes);
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
                    requestContext.abortWith(badClientRequest());
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

            String csrfToken = (String) reactiveRequestContext.getFormParameter(config.formFieldName, true, true);

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
                }
            }
        } else if (cookieToken == null) {
            LOG.debug("CSRF token is not found");
            requestContext.abortWith(badClientRequest());
        }
    }

    private SecureRandom getSecureRandom() {
        return (SecureRandom) SECURE_RANDOM_VH.getAcquire(this);
    }

    private static boolean isMatchingMediaType(MediaType contentType, MediaType expectedType) {
        return contentType.getType().equals(expectedType.getType())
                && contentType.getSubtype().equals(expectedType.getSubtype());
    }

    private static Response badClientRequest() {
        return Response.status(400).build();
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

    public void configure(CsrfReactiveConfig configuration) {
        this.config = configuration;
        SECURE_RANDOM_VH.setRelease(this, new SecureRandom());
    }

    @Override
    public Class<CsrfReactiveConfig> getConfigurationClass() {
        return CsrfReactiveConfig.class;
    }
}
