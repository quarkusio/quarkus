package io.quarkus.smallrye.openapi.runtime.filter;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Add OAuth 2 (Implicit) Authentication security automatically based on the added security extensions
 */
public class OpenIDConnectSecurityFilter extends AutoSecurityFilter {

    private AutoUrl authorizationUrl;
    private AutoUrl refreshUrl;
    private AutoUrl tokenUrl;

    public OpenIDConnectSecurityFilter() {
        super();
    }

    public OpenIDConnectSecurityFilter(String securitySchemeName, String securitySchemeDescription,
            AutoUrl authorizationUrl,
            AutoUrl refreshUrl,
            AutoUrl tokenUrl) {
        super(securitySchemeName, securitySchemeDescription);
        this.authorizationUrl = authorizationUrl;
        this.refreshUrl = refreshUrl;
        this.tokenUrl = tokenUrl;
    }

    public AutoUrl getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(AutoUrl authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public AutoUrl getRefreshUrl() {
        return refreshUrl;
    }

    public void setRefreshUrl(AutoUrl refreshUrl) {
        this.refreshUrl = refreshUrl;
    }

    public AutoUrl getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(AutoUrl tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    @Override
    protected SecurityScheme getSecurityScheme() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();

        securityScheme.setType(SecurityScheme.Type.OAUTH2);
        OAuthFlows oAuthFlows = OASFactory.createOAuthFlows();
        OAuthFlow oAuthFlow = OASFactory.createOAuthFlow();
        oAuthFlow.authorizationUrl(this.authorizationUrl.getFinalUrlValue());
        oAuthFlow.refreshUrl(this.refreshUrl.getFinalUrlValue());
        oAuthFlow.tokenUrl(this.tokenUrl.getFinalUrlValue());
        oAuthFlows.setImplicit(oAuthFlow);
        securityScheme.setFlows(oAuthFlows);

        return securityScheme;
    }

}