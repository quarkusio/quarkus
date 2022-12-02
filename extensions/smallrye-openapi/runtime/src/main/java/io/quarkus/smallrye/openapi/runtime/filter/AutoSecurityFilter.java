package io.quarkus.smallrye.openapi.runtime.filter;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.jboss.logging.Logger;

/**
 * Auto add security
 */
public abstract class AutoSecurityFilter implements OASFilter {
    private static final Logger log = Logger.getLogger(AutoSecurityFilter.class);

    private String securitySchemeName;
    private String securitySchemeDescription;

    public AutoSecurityFilter() {

    }

    public AutoSecurityFilter(String securitySchemeName, String securitySchemeDescription) {
        this.securitySchemeName = securitySchemeName;
        this.securitySchemeDescription = securitySchemeDescription;
    }

    public String getSecuritySchemeName() {
        return securitySchemeName;
    }

    public void setSecuritySchemeName(String securitySchemeName) {
        this.securitySchemeName = securitySchemeName;
    }

    public String getSecuritySchemeDescription() {
        return securitySchemeDescription;
    }

    public void setSecuritySchemeDescription(String securitySchemeDescription) {
        this.securitySchemeDescription = securitySchemeDescription;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        // Make sure components are created
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }

        Map<String, SecurityScheme> securitySchemes = new HashMap<>();

        // Add any existing security
        if (openAPI.getComponents().getSecuritySchemes() != null
                && !openAPI.getComponents().getSecuritySchemes().isEmpty()) {
            securitySchemes.putAll(openAPI.getComponents().getSecuritySchemes());
        }

        SecurityScheme securityScheme = getSecurityScheme();
        securityScheme.setDescription(securitySchemeDescription);
        securitySchemes.put(securitySchemeName, securityScheme);
        openAPI.getComponents().setSecuritySchemes(securitySchemes);
    }

    protected abstract SecurityScheme getSecurityScheme();

    protected String getUrl(String configKey, String defaultValue, String shouldEndWith) {
        Config c = ConfigProvider.getConfig();

        String u = c.getOptionalValue(configKey, String.class).orElse(defaultValue);

        if (u != null && !u.endsWith(shouldEndWith)) {
            u = u + shouldEndWith;
        }
        return u;
    }

}