package io.quarkus.oidc;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

/**
 * OIDC redirect filter which can be used to customize redirect requests to OIDC authorization and logout endpoints
 * as well as local redirects to OIDC tenant error, session expired and other pages.
 */
public interface OidcRedirectFilter {

    /**
     * OIDC redirect context which provides access to the routing context, current OIDC tenant configuration, redirect uri
     * and additional query parameters.
     * The additional query parameters are visible to all OIDC redirect filters. They are URL-encoded and added to
     * the redirect URI after all the filters have run.
     */
    record OidcRedirectContext(RoutingContext routingContext, OidcTenantConfig oidcTenantConfig,
            String redirectUri, MultiMap additionalQueryParams) {
    }

    /**
     * Filter OIDC redirect.
     *
     * @param redirectContext the redirect context which provides access to the routing context, current OIDC tenant
     *        configuration, redirect uri and additional query parameters.
     *
     */
    void filter(OidcRedirectContext redirectContext);
}
