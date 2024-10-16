package io.quarkus.smallrye.openapi.runtime.filter;

import java.util.Map;

import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Add JWT Authentication security automatically based on the added security extensions
 */
public class AutoBearerTokenSecurityFilter extends AutoSecurityFilter {
    private String securitySchemeValue;
    private String bearerFormat;

    public AutoBearerTokenSecurityFilter() {
        super();
    }

    public AutoBearerTokenSecurityFilter(String securitySchemeName, String securitySchemeDescription,
            Map<String, String> securitySchemeExtensions,
            String securitySchemeValue,
            String bearerFormat) {
        super(securitySchemeName, securitySchemeDescription, securitySchemeExtensions);
        this.securitySchemeValue = securitySchemeValue;
        this.bearerFormat = bearerFormat;
    }

    public String getSecuritySchemeValue() {
        return securitySchemeValue;
    }

    public void setSecuritySchemeValue(String securitySchemeValue) {
        this.securitySchemeValue = securitySchemeValue;
    }

    public String getBearerFormat() {
        return bearerFormat;
    }

    public void setBearerFormat(String bearerFormat) {
        this.bearerFormat = bearerFormat;
    }

    @Override
    protected void updateSecurityScheme(SecurityScheme securityScheme) {
        securityScheme.setType(SecurityScheme.Type.HTTP);
        securityScheme.setScheme(securitySchemeValue);
        securityScheme.setBearerFormat(bearerFormat);
    }
}
