package io.quarkus.csrf.reactive.runtime;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.vertx.core.http.Cookie;
import io.vertx.core.http.impl.CookieImpl;
import io.vertx.core.http.impl.ServerCookie;
import io.vertx.ext.web.RoutingContext;

public class CsrfResponseFilter {
    private static final Logger LOG = Logger.getLogger(CsrfResponseFilter.class);

    /**
     * CSRF token key.
     */
    private static final String CSRF_TOKEN_KEY = "csrf_token";
    private static final String CSRF_TOKEN_BYTES_KEY = "csrf_token_bytes";

    @Inject
    Instance<CsrfReactiveConfig> config;

    public CsrfResponseFilter() {
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
        if (requestContext.getMethod().equals("GET") && isCsrfTokenRequired(routing, config.get())
                && getCookieToken(routing, config.get()) == null) {

            String cookieValue = null;
            if (config.get().tokenSignatureKey.isPresent()) {
                byte[] csrfTokenBytes = (byte[]) routing.get(CSRF_TOKEN_BYTES_KEY);

                if (csrfTokenBytes == null) {
                    throw new IllegalStateException(
                            "CSRF Filter should have set the property " + CSRF_TOKEN_KEY + ", but it is null");
                }
                cookieValue = CsrfTokenUtils.signCsrfToken(csrfTokenBytes, config.get().tokenSignatureKey.get());
            } else {
                String csrfToken = (String) routing.get(CSRF_TOKEN_KEY);

                if (csrfToken == null) {
                    throw new IllegalStateException(
                            "CSRF Filter should have set the property " + CSRF_TOKEN_KEY + ", but it is null");
                }
                cookieValue = csrfToken;
            }

            createCookie(cookieValue, routing, config.get());
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

}
