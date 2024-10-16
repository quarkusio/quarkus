package io.quarkus.smallrye.openapi.common.deployment;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
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
     * Do not run the filter only at startup, but every time the document is requested (dynamic).
     */
    @ConfigItem(defaultValue = "false")
    public boolean alwaysRunFilter;

    /**
     * Do not include the provided static openapi document (eg. META-INF/openapi.yaml)
     */
    @ConfigItem(defaultValue = "false")
    public boolean ignoreStaticDocument;

    /**
     * If management interface is turned on the openapi schema document will be published under the management interface. This
     * allows you to exclude OpenAPI from management by setting the value to false
     */
    @ConfigItem(name = "management.enabled", defaultValue = "true")
    public boolean managementEnabled;

    /**
     * A list of local directories that should be scanned for yaml and/or json files to be included in the static model.
     * Example: `META-INF/openapi/`
     */
    @ConfigItem
    public Optional<List<Path>> additionalDocsDirectory;

    /**
     * Add a certain SecurityScheme with config
     */
    @ConfigItem
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
     * Add one or more extensions to the security scheme
     */
    @ConfigItem
    @ConfigDocMapKey("extension-name")
    public Map<String, String> securitySchemeExtensions = Collections.emptyMap();

    /**
     * This will automatically add the security requirement to all methods/classes that has a `RolesAllowed` annotation.
     */
    @ConfigItem(defaultValue = "true")
    public boolean autoAddSecurityRequirement;

    /**
     * This will automatically add tags to operations based on the Java class name.
     */
    @ConfigItem(defaultValue = "true")
    public boolean autoAddTags;

    /**
     * Setting it to `true` will automatically add a default server to the schema if none is provided,
     * using the current running server host and port.
     */
    @ConfigItem
    public Optional<Boolean> autoAddServer;

    /**
     * This will automatically add security based on the security extension included (if any).
     */
    @ConfigItem(defaultValue = "true")
    public boolean autoAddSecurity;

    /**
     * Required when using `apiKey` security. The location of the API key. Valid values are "query", "header" or "cookie".
     */
    @ConfigItem
    public Optional<String> apiKeyParameterIn;

    /**
     * Required when using `apiKey` security. The name of the header, query or cookie parameter to be used.
     */
    @ConfigItem
    public Optional<String> apiKeyParameterName;

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
     * Add a bearer format the JWT Security Scheme
     */
    @ConfigItem(defaultValue = "JWT")
    public String jwtBearerFormat;

    /**
     * Add a scheme value to the OAuth2 opaque token Security Scheme
     */
    @ConfigItem(defaultValue = "bearer")
    public String oauth2SecuritySchemeValue;

    /**
     * Add a scheme value to OAuth2 opaque token Security Scheme
     */
    @ConfigItem(defaultValue = "Opaque")
    public String oauth2BearerFormat;

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

    /**
     * Override the openapi version in the Schema document
     */
    @ConfigItem
    public Optional<String> openApiVersion;

    /**
     * Set the title in Info tag in the Schema document
     */
    @ConfigItem
    public Optional<String> infoTitle;

    /**
     * Set the version in Info tag in the Schema document
     */
    @ConfigItem
    public Optional<String> infoVersion;

    /**
     * Set the description in Info tag in the Schema document
     */
    @ConfigItem
    public Optional<String> infoDescription;

    /**
     * Set the terms of the service in Info tag in the Schema document
     */
    @ConfigItem
    public Optional<String> infoTermsOfService;

    /**
     * Set the contact email in Info tag in the Schema document
     */
    @ConfigItem
    public Optional<String> infoContactEmail;

    /**
     * Set the contact name in Info tag in the Schema document
     */
    @ConfigItem
    public Optional<String> infoContactName;

    /**
     * Set the contact url in Info tag in the Schema document
     */
    @ConfigItem
    public Optional<String> infoContactUrl;

    /**
     * Set the license name in Info tag in the Schema document
     */
    @ConfigItem
    public Optional<String> infoLicenseName;

    /**
     * Set the license url in Info tag in the Schema document
     */
    @ConfigItem
    public Optional<String> infoLicenseUrl;

    /**
     * Set the strategy to automatically create an operation Id
     */
    @ConfigItem
    public Optional<OperationIdStrategy> operationIdStrategy;

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

    public Map<String, String> getValidSecuritySchemeExtentions() {
        return securitySchemeExtensions
                .entrySet()
                .stream()
                .filter(x -> x.getKey().startsWith("x-"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
