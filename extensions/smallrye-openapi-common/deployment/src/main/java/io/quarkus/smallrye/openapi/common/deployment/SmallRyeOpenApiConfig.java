package io.quarkus.smallrye.openapi.common.deployment;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.smallrye-openapi")
public interface SmallRyeOpenApiConfig {

    String DEFAULT_DOCUMENT_NAME = "<default>";

    String DEFAULT_PATH = "openapi-<document-name>";

    String DEFAULT_STORE_SCHEMA_FILE_NAME = "openapi-<document-name>";

    /**
     * The path at which to register the OpenAPI Servlet.
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default String path() {
        return documents().get(DEFAULT_DOCUMENT_NAME).path();
    }

    /**
     * If set, the generated OpenAPI schema documents will be stored here on build.
     * Both openapi.json and openapi.yaml will be stored here if this is set.
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<Path> storeSchemaDirectory() {
        return documents().get(DEFAULT_DOCUMENT_NAME).storeSchemaDirectory();
    }

    /**
     * The name of the file in case it is being stored.
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default String storeSchemaFileName() {
        return documents().get(DEFAULT_DOCUMENT_NAME).storeSchemaFileName();
    }

    /**
     * Do not run the filter only at startup, but every time the document is requested (dynamic).
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default boolean alwaysRunFilter() {
        return documents().get(DEFAULT_DOCUMENT_NAME).alwaysRunFilter();
    }

    /**
     * Do not include the provided static openapi document (eg. META-INF/openapi.yaml)
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default boolean ignoreStaticDocument() {
        return documents().get(DEFAULT_DOCUMENT_NAME).ignoreStaticDocument();
    }

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
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<List<Path>> additionalDocsDirectory() {
        return documents().get(DEFAULT_DOCUMENT_NAME).additionalDocsDirectory();
    }

    /**
     * Add a certain SecurityScheme with config
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<SecurityScheme> securityScheme() {
        return documents().get(DEFAULT_DOCUMENT_NAME).securityScheme();
    }

    /**
     * Add a Security Scheme name to the generated OpenAPI document
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default String securitySchemeName() {
        return documents().get(DEFAULT_DOCUMENT_NAME).securitySchemeName();
    }

    /**
     * Add a description to the Security Scheme
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default String securitySchemeDescription() {
        return documents().get(DEFAULT_DOCUMENT_NAME).securitySchemeDescription();
    }

    /**
     * Add one or more extensions to the security scheme
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Map<String, String> securitySchemeExtensions() {
        return documents().get(DEFAULT_DOCUMENT_NAME).securitySchemeExtensions();
    }

    /**
     * This will automatically add the security requirement to all methods/classes that has a `RolesAllowed` annotation.
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default boolean autoAddSecurityRequirement() {
        return documents().get(DEFAULT_DOCUMENT_NAME).autoAddSecurityRequirement();
    }

    /**
     * This will automatically add tags to operations based on the Java class name.
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default boolean autoAddTags() {
        return documents().get(DEFAULT_DOCUMENT_NAME).autoAddTags();
    }

    /**
     * This will automatically add Bad Request (400 HTTP response) API response to operations with an input.
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default boolean autoAddBadRequestResponse() {
        return documents().get(DEFAULT_DOCUMENT_NAME).autoAddBadRequestResponse();
    }

    /**
     * This will automatically add a summary to operations based on the Java method name.
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default boolean autoAddOperationSummary() {
        return documents().get(DEFAULT_DOCUMENT_NAME).autoAddOperationSummary();
    }

    /**
     * Setting it to `true` will automatically add a default server to the schema if none is provided,
     * using the current running server host and port.
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<Boolean> autoAddServer() {
        return documents().get(DEFAULT_DOCUMENT_NAME).autoAddServer();
    }

    /**
     * This will automatically add security based on the security extension included (if any).
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default boolean autoAddSecurity() {
        return documents().get(DEFAULT_DOCUMENT_NAME).autoAddSecurity();
    }

    /**
     * This will automatically add the OpenAPI specification document endpoint to the schema.
     * It also adds "openapi" to the list of tags and specify an "operationId"
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default boolean autoAddOpenApiEndpoint() {
        return documents().get(DEFAULT_DOCUMENT_NAME).autoAddOpenApiEndpoint();
    }

    /**
     * Required when using `apiKey` security. The location of the API key. Valid values are "query", "header" or "cookie".
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> apiKeyParameterIn() {
        return documents().get(DEFAULT_DOCUMENT_NAME).apiKeyParameterIn();
    }

    /**
     * Required when using `apiKey` security. The name of the header, query or cookie parameter to be used.
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> apiKeyParameterName() {
        return documents().get(DEFAULT_DOCUMENT_NAME).apiKeyParameterName();
    }

    /**
     * Add a scheme value to the Basic HTTP Security Scheme
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default String basicSecuritySchemeValue() {
        return documents().get(DEFAULT_DOCUMENT_NAME).basicSecuritySchemeValue();
    }

    /**
     * Add a scheme value to the JWT Security Scheme
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default String jwtSecuritySchemeValue() {
        return documents().get(DEFAULT_DOCUMENT_NAME).jwtSecuritySchemeValue();
    }

    /**
     * Add a bearer format the JWT Security Scheme
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default String jwtBearerFormat() {
        return documents().get(DEFAULT_DOCUMENT_NAME).jwtBearerFormat();
    }

    /**
     * Add a scheme value to the OAuth2 opaque token Security Scheme
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default String oauth2SecuritySchemeValue() {
        return documents().get(DEFAULT_DOCUMENT_NAME).oauth2SecuritySchemeValue();
    }

    /**
     * Add a scheme value to OAuth2 opaque token Security Scheme
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default String oauth2BearerFormat() {
        return documents().get(DEFAULT_DOCUMENT_NAME).oauth2BearerFormat();
    }

    /**
     * Add a openIdConnectUrl value to the OIDC Security Scheme
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> oidcOpenIdConnectUrl() {
        return documents().get(DEFAULT_DOCUMENT_NAME).oidcOpenIdConnectUrl();
    }

    /**
     * Add a implicit flow refreshUrl value to the OAuth2 Security Scheme
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> oauth2ImplicitRefreshUrl() {
        return documents().get(DEFAULT_DOCUMENT_NAME).oauth2ImplicitRefreshUrl();
    }

    /**
     * Add an implicit flow authorizationUrl value to the OAuth2 Security Scheme
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> oauth2ImplicitAuthorizationUrl() {
        return documents().get(DEFAULT_DOCUMENT_NAME).oauth2ImplicitAuthorizationUrl();
    }

    /**
     * Add an implicit flow tokenUrl value to the OAuth2 Security Scheme
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> oauth2ImplicitTokenUrl() {
        return documents().get(DEFAULT_DOCUMENT_NAME).oauth2ImplicitTokenUrl();
    }

    /**
     * Override the openapi version in the Schema document
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> openApiVersion() {
        return documents().get(DEFAULT_DOCUMENT_NAME).openApiVersion();
    }

    /**
     * Set the title in Info tag in the Schema document
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> infoTitle() {
        return documents().get(DEFAULT_DOCUMENT_NAME).infoTitle();
    }

    /**
     * Set the version in Info tag in the Schema document
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> infoVersion() {
        return documents().get(DEFAULT_DOCUMENT_NAME).infoVersion();
    }

    /**
     * Set the description in Info tag in the Schema document
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> infoDescription() {
        return documents().get(DEFAULT_DOCUMENT_NAME).infoDescription();
    }

    /**
     * Set the terms of the service in Info tag in the Schema document
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> infoTermsOfService() {
        return documents().get(DEFAULT_DOCUMENT_NAME).infoTermsOfService();
    }

    /**
     * Set the contact email in Info tag in the Schema document
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> infoContactEmail() {
        return documents().get(DEFAULT_DOCUMENT_NAME).infoContactEmail();
    }

    /**
     * Set the contact name in Info tag in the Schema document
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> infoContactName() {
        return documents().get(DEFAULT_DOCUMENT_NAME).infoContactName();
    }

    /**
     * Set the contact url in Info tag in the Schema document
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> infoContactUrl() {
        return documents().get(DEFAULT_DOCUMENT_NAME).infoContactUrl();
    }

    /**
     * Set the license name in Info tag in the Schema document
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> infoLicenseName() {
        return documents().get(DEFAULT_DOCUMENT_NAME).infoLicenseName();
    }

    /**
     * Set the license url in Info tag in the Schema document
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> infoLicenseUrl() {
        return documents().get(DEFAULT_DOCUMENT_NAME).infoLicenseUrl();
    }

    /**
     * Set the strategy to automatically create an operation Id. The strategy may be
     * one of the predefined values or the name of a fully-qualified class that
     * implements the {@code io.smallrye.openapi.api.OperationIdGenerator}
     * interface.
     *
     * <p>
     * Predefined strategies:
     * <ul>
     * <li>{@code method}: generate an operationId with the resource method's
     * name</li>
     * <li>{@code class-method}: generate an operationId with the resource class's
     * simple name and the resource method's name</li>
     * <li>{@code package-class-method}: generate an operationId with the resource
     * class's fully-qualified name and the resource method's name</li>
     * </ul>
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Optional<String> operationIdStrategy() {
        return documents().get(DEFAULT_DOCUMENT_NAME).operationIdStrategy();
    }

    /**
     * Set this boolean value to enable or disable the merging of the deprecated `@Schema`
     * `example` property into the `examples` array introduced in OAS 3.1.0. If
     * set to `false`, the deprecated `example` will be kept as a separate
     * annotation on the schema in the OpenAPI model.
     *
     * @deprecated Use {@link #documents()} with key {@link #DEFAULT_DOCUMENT_NAME} instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default boolean mergeSchemaExamples() {
        return documents().get(DEFAULT_DOCUMENT_NAME).mergeSchemaExamples();
    }

    enum SecurityScheme {
        apiKey,
        basic,
        jwt,
        oauth2,
        oidc,
        oauth2Implicit
    }

    /**
     * Get valid security scheme extensions (those starting with "x-")
     *
     * @deprecated Use {@link OpenApiDocumentConfig#getValidSecuritySchemeExtensions()} with key {@link #DEFAULT_DOCUMENT_NAME}
     *             instead
     */
    @Deprecated(since = "3.31", forRemoval = true)
    default Map<String, String> getValidSecuritySchemeExtensions() {
        return documents().get(DEFAULT_DOCUMENT_NAME).getValidSecuritySchemeExtensions();
    }

    /**
     * OpenAPI documents
     */
    @ConfigDocMapKey("document-name")
    @WithParentName
    @WithUnnamedKey(DEFAULT_DOCUMENT_NAME)
    @WithDefaults
    Map<String, OpenApiDocumentConfig> documents();
}
