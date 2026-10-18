package io.quarkus.smallrye.openapi.runtime.filter;

import java.util.Map;

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
            Map<String, String> securitySchemeExtensions,
            AutoUrl openIdConnectUrl) {
        super(securitySchemeName, securitySchemeDescription, securitySchemeExtensions);
        this.openIdConnectUrl = openIdConnectUrl;
    }

    public AutoUrl getOpenIdConnectUrl() {
        return openIdConnectUrl;
    }

    public void setOpenIdConnectUrl(AutoUrl openIdConnectUrl) {
        this.openIdConnectUrl = openIdConnectUrl;
    }

    @Override
    public boolean runtimeRequired() {
        return true;
    }

    @Override
    protected void updateSecurityScheme(SecurityScheme securityScheme) {
        securityScheme.setType(SecurityScheme.Type.OPENIDCONNECT);
        securityScheme.setOpenIdConnectUrl(openIdConnectUrl.getFinalUrlValue());
    }

}
