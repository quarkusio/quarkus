package io.quarkus.smallrye.openapi.runtime.filter;

import java.util.Map;

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
            Map<String, String> securitySchemeExtensions,
            String basicSecuritySchemeValue) {
        super(securitySchemeName, securitySchemeDescription, securitySchemeExtensions);
        this.basicSecuritySchemeValue = basicSecuritySchemeValue;
    }

    public String getBasicSecuritySchemeValue() {
        return basicSecuritySchemeValue;
    }

    public void setBasicSecuritySchemeValue(String basicSecuritySchemeValue) {
        this.basicSecuritySchemeValue = basicSecuritySchemeValue;
    }

    @Override
    protected void updateSecurityScheme(SecurityScheme securityScheme) {
        securityScheme.setType(SecurityScheme.Type.HTTP);
        securityScheme.setScheme(basicSecuritySchemeValue);
    }
}