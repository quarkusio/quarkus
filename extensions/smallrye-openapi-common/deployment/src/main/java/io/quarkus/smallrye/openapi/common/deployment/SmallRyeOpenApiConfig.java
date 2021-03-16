package io.quarkus.smallrye.openapi.common.deployment;

import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-openapi")
public final class SmallRyeOpenApiConfig {
    /**
     * The path at which to register the OpenAPI Servlet.
     */
    @ConfigItem(defaultValue = "openapi")
    public String path;

    /**
     * If set, the generated OpenAPI schema documents will be stored here on build.
     * Both openapi.json and openapi.yaml will be stored here if this is set.
     */
    @ConfigItem
    public Optional<Path> storeSchemaDirectory;

    /**
     * Add a certain SecurityScheme with config
     */
    public Optional<SecurityScheme> securityScheme;

    /**
     * Add a Security Scheme name to the generated OpenAPI document
     */
    @ConfigItem(defaultValue = "SecurityScheme")
    public String securitySchemeName;

    /**
     * Add a description to the Security Scheme
     */
    @ConfigItem(defaultValue = "Authentication")
    public String securitySchemeDescription;

    /**
     * Add a scheme value to the Basic HTTP Security Scheme
     */
    @ConfigItem(defaultValue = "basic")
    public String basicSecuritySchemeValue;

    /**
     * Add a scheme value to the JWT Security Scheme
     */
    @ConfigItem(defaultValue = "bearer")
    public String jwtSecuritySchemeValue;

    /**
     * Add a scheme value to the JWT Security Scheme
     */
    @ConfigItem(defaultValue = "JWT")
    public String jwtBearerFormat;

    /**
     * Add a openIdConnectUrl value to the OIDC Security Scheme
     */
    @ConfigItem
    public Optional<String> oidcOpenIdConnectUrl;

    /**
     * Add a implicit flow refreshUrl value to the OAuth2 Security Scheme
     */
    @ConfigItem
    public Optional<String> oauth2ImplicitRefreshUrl;

    /**
     * Add an implicit flow authorizationUrl value to the OAuth2 Security Scheme
     */
    @ConfigItem
    public Optional<String> oauth2ImplicitAuthorizationUrl;

    /**
     * Add an implicit flow tokenUrl value to the OAuth2 Security Scheme
     */
    @ConfigItem
    public Optional<String> oauth2ImplicitTokenUrl;

    public enum SecurityScheme {
        basic,
        jwt,
        oidc,
        oauth2Implicit
    }
}
