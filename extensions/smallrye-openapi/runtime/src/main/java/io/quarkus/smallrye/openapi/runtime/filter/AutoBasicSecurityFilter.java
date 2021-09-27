package io.quarkus.smallrye.openapi.runtime.filter;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Add Basic Authentication security automatically based on the added security extensions
 */
public class AutoBasicSecurityFilter extends AutoSecurityFilter {
    private String basicSecuritySchemeValue;

    public AutoBasicSecurityFilter() {
        super();
    }

    public AutoBasicSecurityFilter(String securitySchemeName, String securitySchemeDescription,
            String basicSecuritySchemeValue) {
        super(securitySchemeName, securitySchemeDescription);
        this.basicSecuritySchemeValue = basicSecuritySchemeValue;
    }

    public String getBasicSecuritySchemeValue() {
        return basicSecuritySchemeValue;
    }

    public void setBasicSecuritySchemeValue(String basicSecuritySchemeValue) {
        this.basicSecuritySchemeValue = basicSecuritySchemeValue;
    }

    @Override
    protected SecurityScheme getSecurityScheme() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        securityScheme.setType(SecurityScheme.Type.HTTP);
        securityScheme.setScheme(basicSecuritySchemeValue);
        return securityScheme;
    }
}