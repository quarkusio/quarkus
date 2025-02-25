package io.quarkus.csrf.reactive.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.vertx.ext.web.RoutingContext;

/**
 * CSRF token form parameter provider which supports the injection of the CSRF token in Qute templates.
 */
@ApplicationScoped
@Named("csrf")
public class CsrfTokenParameterProvider {
    /**
     * CSRF token key.
     */
    private static final String CSRF_TOKEN_KEY = "csrf_token";

    @Inject
    RoutingContext context;

    private final String csrfFormFieldName;
    private final String csrfCookieName;
    private final String csrfHeaderName;

    public CsrfTokenParameterProvider(RestCsrfConfig config) {
        this.csrfFormFieldName = config.formFieldName();
        this.csrfCookieName = config.cookieName();
        this.csrfHeaderName = config.tokenHeaderName();
    }

    /**
     * Gets the CSRF token value.
     *
     * @throws IllegalStateException if the {@link RoutingContext} does not contain a CSRF token value.
     */
    public String getToken() {
        String token = (String) context.get(CSRF_TOKEN_KEY);

        if (token == null) {
            throw new IllegalStateException(
                    "CSRFFilter should have set the attribute " + csrfFormFieldName + ", but it is null");
        }

        return token;
    }

    /**
     * Gets the name of the form parameter that is to contain the value returned by {@link #getToken()}.
     */
    public String getParameterName() {
        return csrfFormFieldName;
    }

    /**
     * Gets the CSRF cookie name.
     */
    public String getCookieName() {
        return csrfCookieName;
    }

    /**
     * Gets the CSRF header name.
     */
    public String getHeaderName() {
        return csrfHeaderName;
    }
}
