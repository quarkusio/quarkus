package io.quarkus.smallrye.openapi.runtime.filter;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Add OAuth 2 (Implicit) Authentication security automatically based on the added security extensions
 */
public class OpenIDConnectSecurityFilter extends AutoSecurityFilter {

    private AutoUrl openIdConnectUrl;

    public OpenIDConnectSecurityFilter() {
        super();
    }

    public OpenIDConnectSecurityFilter(String securitySchemeName, String securitySchemeDescription,
            AutoUrl openIdConnectUrl) {
        super(securitySchemeName, securitySchemeDescription);
        this.openIdConnectUrl = openIdConnectUrl;
    }

    public AutoUrl getOpenIdConnectUrl() {
        return openIdConnectUrl;
    }

    public void setOpenIdConnectUrl(AutoUrl openIdConnectUrl) {
        this.openIdConnectUrl = openIdConnectUrl;
    }

    @Override
    protected SecurityScheme getSecurityScheme() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        securityScheme.setType(SecurityScheme.Type.OPENIDCONNECT);
        securityScheme.setOpenIdConnectUrl(openIdConnectUrl.getFinalUrlValue());
        return securityScheme;
    }

}
