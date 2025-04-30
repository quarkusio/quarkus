package io.quarkus.oidc;

import io.vertx.ext.web.RoutingContext;

/**
 * JavaScriptRequestChecker can be used to check if the current request was made
 * by JavaScript running inside Single-page application (SPA).
 * <p/>
 * Some OpenId Connect providers may not support CORS in their authorization endpoints.
 * In such cases, SPA needs to avoid using JavaScript for running authorization code flow redirects
 * and instead delegate it to the browser.
 * <p/>
 * If this checker confirms it is a JavaScript request and if authentication challenge redirects are also disabled with
 * 'quarkus.oidc.authentication.java-script-auto-redirect=false' then an HTTP error status `499` will be reported allowing
 * SPA to intercept this error and repeat the last request causing the challenge with the browser API.
 */
public interface JavaScriptRequestChecker {
    /**
     * Check if the current request was made by JavaScript
     *
     * @param context {@link RoutingContext}
     * @return true if the current request was made by JavaScript
     */
    boolean isJavaScriptRequest(RoutingContext context);
}
