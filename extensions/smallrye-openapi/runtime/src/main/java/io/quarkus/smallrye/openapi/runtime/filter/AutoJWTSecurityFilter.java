package io.quarkus.smallrye.openapi.runtime.filter;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Add JWT Authentication security automatically based on the added security extensions
 */
public class AutoJWTSecurityFilter extends AutoSecurityFilter {
    private String jwtSecuritySchemeValue;
    private String jwtBearerFormat;

    public AutoJWTSecurityFilter() {
        super();
    }

    public AutoJWTSecurityFilter(String securitySchemeName, String securitySchemeDescription, String jwtSecuritySchemeValue,
            String jwtBearerFormat) {
        super(securitySchemeName, securitySchemeDescription);
        this.jwtSecuritySchemeValue = jwtSecuritySchemeValue;
        this.jwtBearerFormat = jwtBearerFormat;
    }

    public String getJwtSecuritySchemeValue() {
        return jwtSecuritySchemeValue;
    }

    public void setJwtSecuritySchemeValue(String jwtSecuritySchemeValue) {
        this.jwtSecuritySchemeValue = jwtSecuritySchemeValue;
    }

    public String getJwtBearerFormat() {
        return jwtBearerFormat;
    }

    public void setJwtBearerFormat(String jwtBearerFormat) {
        this.jwtBearerFormat = jwtBearerFormat;
    }

    @Override
    protected SecurityScheme getSecurityScheme() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        securityScheme.setType(SecurityScheme.Type.HTTP);
        securityScheme.setScheme(jwtSecuritySchemeValue);
        securityScheme.setBearerFormat(jwtBearerFormat);
        return securityScheme;
    }
}
