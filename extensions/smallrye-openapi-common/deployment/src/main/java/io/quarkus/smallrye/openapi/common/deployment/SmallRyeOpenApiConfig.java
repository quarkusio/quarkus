package io.quarkus.smallrye.openapi.common.deployment;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.smallrye-openapi")
public interface SmallRyeOpenApiConfig {
    /**
     * The path at which to register the OpenAPI Servlet.
     */
    @WithDefault("openapi")
    String path();

    /**
     * If set, the generated OpenAPI schema documents will be stored here on build.
     * Both openapi.json and openapi.yaml will be stored here if this is set.
     */
    Optional<Path> storeSchemaDirectory();

    /**
     * The name of the file in case it is being stored.
     */
    @WithDefault("openapi")
    String storeSchemaFileName();

    /**
     * Do not run the filter only at startup, but every time the document is requested (dynamic).
     */
    @WithDefault("false")
    boolean alwaysRunFilter();

    /**
     * Do not include the provided static openapi document (eg. META-INF/openapi.yaml)
     */
    @WithDefault("false")
    boolean ignoreStaticDocument();

    /**
     * If management interface is turned on the openapi schema document will be published under the management interface. This
     * allows you to exclude OpenAPI from management by setting the value to false
     */
    @WithName("management.enabled")
    @WithDefault("true")
    boolean managementEnabled();

    /**
     * A list of local directories that should be scanned for yaml and/or json files to be included in the static model.
     * Example: `META-INF/openapi/`
     */
    Optional<List<Path>> additionalDocsDirectory();

    /**
     * Add a certain SecurityScheme with config
     */
    Optional<SecurityScheme> securityScheme();

    /**
     * Add a Security Scheme name to the generated OpenAPI document
     */
    @WithDefault("SecurityScheme")
    String securitySchemeName();

    /**
     * Add a description to the Security Scheme
     */
    @WithDefault("Authentication")
    String securitySchemeDescription();

    /**
     * Add one or more extensions to the security scheme
     */
    @ConfigDocMapKey("extension-name")
    Map<String, String> securitySchemeExtensions();

    /**
     * This will automatically add the security requirement to all methods/classes that has a `RolesAllowed` annotation.
     */
    @WithDefault("true")
    boolean autoAddSecurityRequirement();

    /**
     * This will automatically add tags to operations based on the Java class name.
     */
    @WithDefault("true")
    boolean autoAddTags();

    /**
     * This will automatically add Bad Request (400 HTTP response) API response to operations with an input.
     */
    @WithDefault("true")
    boolean autoAddBadRequestResponse();

    /**
     * This will automatically add a summary to operations based on the Java method name.
     */
    @WithDefault("true")
    boolean autoAddOperationSummary();

    /**
     * Setting it to `true` will automatically add a default server to the schema if none is provided,
     * using the current running server host and port.
     */
    Optional<Boolean> autoAddServer();

    /**
     * This will automatically add security based on the security extension included (if any).
     */
    @WithDefault("true")
    boolean autoAddSecurity();

    /**
     * This will automatically add the OpenAPI specification document endpoint to the schema.
     * It also adds "openapi" to the list of tags and specify an "operationId"
     */
    @WithDefault("false")
    boolean autoAddOpenApiEndpoint();

    /**
     * Required when using `apiKey` security. The location of the API key. Valid values are "query", "header" or "cookie".
     */
    Optional<String> apiKeyParameterIn();

    /**
     * Required when using `apiKey` security. The name of the header, query or cookie parameter to be used.
     */
    Optional<String> apiKeyParameterName();

    /**
     * Add a scheme value to the Basic HTTP Security Scheme
     */
    @WithDefault("basic")
    String basicSecuritySchemeValue();

    /**
     * Add a scheme value to the JWT Security Scheme
     */
    @WithDefault("bearer")
    String jwtSecuritySchemeValue();

    /**
     * Add a bearer format the JWT Security Scheme
     */
    @WithDefault("JWT")
    String jwtBearerFormat();

    /**
     * Add a scheme value to the OAuth2 opaque token Security Scheme
     */
    @WithDefault("bearer")
    String oauth2SecuritySchemeValue();

    /**
     * Add a scheme value to OAuth2 opaque token Security Scheme
     */
    @WithDefault("Opaque")
    String oauth2BearerFormat();

    /**
     * Add a openIdConnectUrl value to the OIDC Security Scheme
     */
    Optional<String> oidcOpenIdConnectUrl();

    /**
     * Add a implicit flow refreshUrl value to the OAuth2 Security Scheme
     */
    Optional<String> oauth2ImplicitRefreshUrl();

    /**
     * Add an implicit flow authorizationUrl value to the OAuth2 Security Scheme
     */
    Optional<String> oauth2ImplicitAuthorizationUrl();

    /**
     * Add an implicit flow tokenUrl value to the OAuth2 Security Scheme
     */
    Optional<String> oauth2ImplicitTokenUrl();

    /**
     * Override the openapi version in the Schema document
     */
    Optional<String> openApiVersion();

    /**
     * Set the title in Info tag in the Schema document
     */
    Optional<String> infoTitle();

    /**
     * Set the version in Info tag in the Schema document
     */
    Optional<String> infoVersion();

    /**
     * Set the description in Info tag in the Schema document
     */
    Optional<String> infoDescription();

    /**
     * Set the terms of the service in Info tag in the Schema document
     */
    Optional<String> infoTermsOfService();

    /**
     * Set the contact email in Info tag in the Schema document
     */
    Optional<String> infoContactEmail();

    /**
     * Set the contact name in Info tag in the Schema document
     */
    Optional<String> infoContactName();

    /**
     * Set the contact url in Info tag in the Schema document
     */
    Optional<String> infoContactUrl();

    /**
     * Set the license name in Info tag in the Schema document
     */
    Optional<String> infoLicenseName();

    /**
     * Set the license url in Info tag in the Schema document
     */
    Optional<String> infoLicenseUrl();

    /**
     * Set the strategy to automatically create an operation Id
     */
    Optional<OperationIdStrategy> operationIdStrategy();

    /**
     * Set this boolean value to enable or disable the merging of the deprecated `@Schema`
     * `example` property into the `examples` array introduced in OAS 3.1.0. If
     * set to `false`, the deprecated `example` will be kept as a separate
     * annotation on the schema in the OpenAPI model.
     */
    @WithDefault("true")
    boolean mergeSchemaExamples();

    public enum SecurityScheme {
        apiKey,
        basic,
        jwt,
        oauth2,
        oidc,
        oauth2Implicit
    }

    public enum OperationIdStrategy {
        METHOD,
        CLASS_METHOD,
        PACKAGE_CLASS_METHOD
    }

    default Map<String, String> getValidSecuritySchemeExtensions() {
        return securitySchemeExtensions()
                .entrySet()
                .stream()
                .filter(x -> x.getKey().startsWith("x-"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
