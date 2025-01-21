package io.quarkus.smallrye.openapi.deployment.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.jboss.logging.Logger;

import io.quarkus.runtime.configuration.ConfigurationException;
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
    /**
     * Add a security scheme from config
     */
    public void filterOpenAPI(OpenAPI openAPI) {
        if (config.securityScheme().isEmpty()) {
            return;
        }

        // Make sure components are created
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }

        Map<String, SecurityScheme> securitySchemes = new HashMap<>();

        // Add any existing security
        Optional.ofNullable(openAPI.getComponents().getSecuritySchemes())
                .ifPresent(securitySchemes::putAll);

        SmallRyeOpenApiConfig.SecurityScheme securitySchemeOption = config.securityScheme().get();
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        securityScheme.setDescription(config.securitySchemeDescription());
        config.getValidSecuritySchemeExtensions().forEach(securityScheme::addExtension);

        switch (securitySchemeOption) {
            case apiKey:
                configureApiKeySecurityScheme(securityScheme);
                break;
            case basic:
                securityScheme.setType(SecurityScheme.Type.HTTP);
                securityScheme.setScheme(config.basicSecuritySchemeValue());
                break;
            case jwt:
                securityScheme.setType(SecurityScheme.Type.HTTP);
                securityScheme.setScheme(config.jwtSecuritySchemeValue());
                securityScheme.setBearerFormat(config.jwtBearerFormat());
                break;
            case oauth2:
                securityScheme.setType(SecurityScheme.Type.HTTP);
                securityScheme.setScheme(config.oauth2SecuritySchemeValue());
                securityScheme.setBearerFormat(config.oauth2BearerFormat());
                break;
            case oidc:
                securityScheme.setType(SecurityScheme.Type.OPENIDCONNECT);
                securityScheme.setOpenIdConnectUrl(config.oidcOpenIdConnectUrl().orElse(null));
                break;
            case oauth2Implicit:
                securityScheme.setType(SecurityScheme.Type.OAUTH2);
                OAuthFlows oAuthFlows = OASFactory.createOAuthFlows();
                OAuthFlow oAuthFlow = OASFactory.createOAuthFlow();
                config.oauth2ImplicitAuthorizationUrl().ifPresent(oAuthFlow::authorizationUrl);
                config.oauth2ImplicitRefreshUrl().ifPresent(oAuthFlow::refreshUrl);
                config.oauth2ImplicitTokenUrl().ifPresent(oAuthFlow::tokenUrl);
                oAuthFlows.setImplicit(oAuthFlow);
                securityScheme.setType(SecurityScheme.Type.OAUTH2);
                securityScheme.setFlows(oAuthFlows);
                break;
        }

        securitySchemes.put(config.securitySchemeName(), securityScheme);
        openAPI.getComponents().setSecuritySchemes(securitySchemes);

        if (securitySchemes.size() > 1) {
            log.warn("Detected multiple Security Schemes, only one scheme is supported at the moment "
                    + securitySchemes.keySet().toString());
        }
    }

    void configureApiKeySecurityScheme(SecurityScheme securityScheme) {
        securityScheme.setType(SecurityScheme.Type.APIKEY);

        securityScheme.setName(config.apiKeyParameterName()
                .orElseThrow(
                        () -> new ConfigurationException("Parameter `name` is required for `apiKey` OpenAPI security scheme")));

        securityScheme.setIn(config.apiKeyParameterIn()
                .map(in -> Stream.of(SecurityScheme.In.values())
                        .filter(v -> v.toString().equals(in))
                        .findFirst()
                        .orElseThrow(() -> new ConfigurationException(
                                "Parameter `in` given for `apiKey` OpenAPI security schema is invalid: [" + in + ']')))
                .orElseThrow(
                        () -> new ConfigurationException("Parameter `in` is required for `apiKey` OpenAPI security scheme")));
    }
}
