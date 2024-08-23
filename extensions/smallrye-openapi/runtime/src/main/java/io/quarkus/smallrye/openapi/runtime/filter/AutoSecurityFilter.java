package io.quarkus.smallrye.openapi.runtime.filter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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
    private Map<String, String> securitySchemeExtensions;

    protected AutoSecurityFilter() {

    }

    protected AutoSecurityFilter(String securitySchemeName, String securitySchemeDescription,
            Map<String, String> securitySchemeExtensions) {
        this.securitySchemeName = securitySchemeName;
        this.securitySchemeDescription = securitySchemeDescription;
        this.securitySchemeExtensions = securitySchemeExtensions;
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

    public Map<String, String> getSecuritySchemeExtensions() {
        return securitySchemeExtensions;
    }

    public void setSecuritySchemeExtensions(Map<String, String> securitySchemeExtensions) {
        this.securitySchemeExtensions = securitySchemeExtensions;
    }

    public boolean runtimeRequired() {
        return false;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        // Make sure components are created
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }

        Map<String, SecurityScheme> securitySchemes = new LinkedHashMap<>();

        // Add any existing security
        Optional.ofNullable(openAPI.getComponents().getSecuritySchemes())
                .ifPresent(securitySchemes::putAll);

        SecurityScheme securityScheme = securitySchemes.computeIfAbsent(
                securitySchemeName,
                name -> OASFactory.createSecurityScheme());

        updateSecurityScheme(securityScheme);

        if (securitySchemeDescription != null) {
            securityScheme.setDescription(securitySchemeDescription);
        }

        securitySchemeExtensions.forEach(securityScheme::addExtension);

        securitySchemes.put(securitySchemeName, securityScheme);
        openAPI.getComponents().setSecuritySchemes(securitySchemes);
    }

    protected abstract void updateSecurityScheme(SecurityScheme securityScheme);

    protected String getUrl(String configKey, String defaultValue, String shouldEndWith) {
        Config c = ConfigProvider.getConfig();

        String u = c.getOptionalValue(configKey, String.class).orElse(defaultValue);

        if (u != null && !u.endsWith(shouldEndWith)) {
            u = u + shouldEndWith;
        }
        return u;
    }

}
