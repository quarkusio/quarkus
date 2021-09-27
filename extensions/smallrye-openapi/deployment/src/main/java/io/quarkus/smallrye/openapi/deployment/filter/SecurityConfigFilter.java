package io.quarkus.smallrye.openapi.deployment.filter;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.jboss.logging.Logger;

import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;

/**
 * Add Basic Security via Config
 */
public class SecurityConfigFilter implements OASFilter {
    private static final Logger log = Logger.getLogger(SecurityConfigFilter.class);

    private final SmallRyeOpenApiConfig config;

    public SecurityConfigFilter(SmallRyeOpenApiConfig config) {
        this.config = config;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {

        // Add a security scheme from config
        if (config.securityScheme.isPresent()) {

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

            SmallRyeOpenApiConfig.SecurityScheme securitySchemeOption = config.securityScheme.get();

            SecurityScheme securityScheme = OASFactory.createSecurityScheme();
            securityScheme.setDescription(config.securitySchemeDescription);

            switch (securitySchemeOption) {
                case basic:
                    securityScheme.setType(SecurityScheme.Type.HTTP);
                    securityScheme.setScheme(config.basicSecuritySchemeValue);
                    break;
                case jwt:
                    securityScheme.setType(SecurityScheme.Type.HTTP);
                    securityScheme.setScheme(config.jwtSecuritySchemeValue);
                    securityScheme.setBearerFormat(config.jwtBearerFormat);
                    break;
                case oidc:
                    securityScheme.setType(SecurityScheme.Type.OPENIDCONNECT);
                    securityScheme.setOpenIdConnectUrl(config.oidcOpenIdConnectUrl.orElse(null));
                    break;
                case oauth2Implicit:
                    securityScheme.setType(SecurityScheme.Type.OAUTH2);
                    OAuthFlows oAuthFlows = OASFactory.createOAuthFlows();
                    OAuthFlow oAuthFlow = OASFactory.createOAuthFlow();
                    if (config.oauth2ImplicitAuthorizationUrl.isPresent()) {
                        oAuthFlow.authorizationUrl(config.oauth2ImplicitAuthorizationUrl.get());
                    }
                    if (config.oauth2ImplicitRefreshUrl.isPresent()) {
                        oAuthFlow.refreshUrl(config.oauth2ImplicitRefreshUrl.get());
                    }
                    if (config.oauth2ImplicitTokenUrl.isPresent()) {
                        oAuthFlow.tokenUrl(config.oauth2ImplicitTokenUrl.get());
                    }
                    oAuthFlows.setImplicit(oAuthFlow);
                    securityScheme.setType(SecurityScheme.Type.OAUTH2);
                    securityScheme.setFlows(oAuthFlows);
                    break;
            }
            securitySchemes.put(config.securitySchemeName, securityScheme);
            openAPI.getComponents().setSecuritySchemes(securitySchemes);
        }

        if (openAPI.getComponents() != null && openAPI.getComponents().getSecuritySchemes() != null) {

            Map<String, SecurityScheme> securitySchemes = openAPI.getComponents().getSecuritySchemes();
            if (securitySchemes.size() > 1) {
                log.warn("Detected multiple Security Schemes, only one scheme is supported at the moment "
                        + securitySchemes.keySet().toString());
            }
        }
    }
}
