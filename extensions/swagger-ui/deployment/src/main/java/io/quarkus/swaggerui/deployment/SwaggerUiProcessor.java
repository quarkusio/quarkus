package io.quarkus.swaggerui.deployment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.builder.Version;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.devui.deployment.menu.EndpointsProcessor;
import io.quarkus.maven.dependency.GACT;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.swaggerui.runtime.SwaggerUiRecorder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarBuildItem;
import io.quarkus.vertx.http.deployment.webjar.WebJarResourcesFilter;
import io.quarkus.vertx.http.deployment.webjar.WebJarResultsBuildItem;
import io.smallrye.openapi.ui.IndexHtmlCreator;
import io.smallrye.openapi.ui.Option;
import io.smallrye.openapi.ui.ThemeHref;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class SwaggerUiProcessor {
    private static final Logger LOG = Logger.getLogger(SwaggerUiProcessor.class);

    private static final GACT SWAGGER_UI_WEBJAR_ARTIFACT_KEY = new GACT("io.smallrye", "smallrye-open-api-ui", null, "jar");
    private static final String SWAGGER_UI_WEBJAR_STATIC_RESOURCES_PATH = "META-INF/resources/openapi-ui/";

    // Branding files to monitor for changes
    private static final String BRANDING_DIR = "META-INF/branding/";
    private static final String BRANDING_LOGO_GENERAL = BRANDING_DIR + "logo.png";
    private static final String BRANDING_LOGO_MODULE = BRANDING_DIR + "smallrye-open-api-ui.png";
    private static final String BRANDING_STYLE_GENERAL = BRANDING_DIR + "style.css";
    private static final String BRANDING_STYLE_MODULE = BRANDING_DIR + "smallrye-open-api-ui.css";
    private static final String BRANDING_FAVICON_GENERAL = BRANDING_DIR + "favicon.ico";
    private static final String BRANDING_FAVICON_MODULE = BRANDING_DIR + "smallrye-open-api-ui.ico";

    // To autoset some security config from OIDC
    private static final String OIDC_CLIENT_ID = "quarkus.oidc.client-id";

    private static final String OIDC_NONCE_KEY = "nonce";

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature,
            LaunchModeBuildItem launchMode,
            SwaggerUiConfig swaggerUiConfig) {
        if (shouldInclude(launchMode, swaggerUiConfig)) {
            feature.produce(new FeatureBuildItem(Feature.SWAGGER_UI));
        }
    }

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> brandingFiles() {
        return Stream.of(BRANDING_LOGO_GENERAL,
                BRANDING_STYLE_GENERAL,
                BRANDING_FAVICON_GENERAL,
                BRANDING_LOGO_MODULE,
                BRANDING_STYLE_MODULE,
                BRANDING_FAVICON_MODULE).map(HotDeploymentWatchedFileBuildItem::new)
                .collect(Collectors.toList());
    }

    @BuildStep
    public void getSwaggerUiFinalDestination(
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            LaunchModeBuildItem launchMode,
            SwaggerUiConfig swaggerUiConfig,
            SmallRyeOpenApiConfig openapi,
            Optional<DevServicesLauncherConfigResultBuildItem> devServicesLauncherConfig,
            BuildProducer<WebJarBuildItem> webJarBuildProducer) throws Exception {

        if (shouldInclude(launchMode, swaggerUiConfig)) {
            if ("/".equals(swaggerUiConfig.path())) {
                throw new ConfigurationException(
                        "quarkus.swagger-ui.path was set to \"/\", this is not allowed as it blocks the application from serving anything else.",
                        Set.of("quarkus.swagger-ui.path"));
            }

            if (openapi.path().equalsIgnoreCase(swaggerUiConfig.path())) {
                throw new ConfigurationException(
                        "quarkus.smallrye-openapi.path and quarkus.swagger-ui.path was set to the same value, this is not allowed as the paths needs to be unique ["
                                + openapi.path() + "].",
                        Set.of("quarkus.smallrye-openapi.path", "quarkus.swagger-ui.path"));

            }

            String openApiPath = nonApplicationRootPathBuildItem.resolvePath(openapi.path());

            String swaggerUiPath = nonApplicationRootPathBuildItem.resolvePath(swaggerUiConfig.path());
            ThemeHref theme = swaggerUiConfig.theme().orElse(ThemeHref.feeling_blue);

            NonApplicationRootPathBuildItem indexRootPathBuildItem = null;

            byte[] indexHtmlContent = generateIndexHtml(openApiPath, swaggerUiPath, swaggerUiConfig,
                    indexRootPathBuildItem,
                    launchMode,
                    devServicesLauncherConfig.orElse(null));
            webJarBuildProducer.produce(
                    WebJarBuildItem.builder().artifactKey(SWAGGER_UI_WEBJAR_ARTIFACT_KEY) //
                            .root(SWAGGER_UI_WEBJAR_STATIC_RESOURCES_PATH) //
                            .filter(new WebJarResourcesFilter() {
                                @Override
                                public FilterResult apply(String fileName, InputStream file) throws IOException {
                                    if (!fileName.equals(theme.toString()) && fileName.startsWith("theme-")) {
                                        return new FilterResult(null, true);
                                    }
                                    if (fileName.endsWith("index.html")) {
                                        return new FilterResult(new ByteArrayInputStream(indexHtmlContent), true);
                                    }
                                    return new FilterResult(file, false);
                                }
                            })
                            .build());
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void registerSwaggerUiHandler(SwaggerUiRecorder recorder,
            BuildProducer<RouteBuildItem> routes,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            WebJarResultsBuildItem webJarResultsBuildItem,
            LaunchModeBuildItem launchMode,
            SwaggerUiConfig swaggerUiConfig,
            BuildProducer<SwaggerUiBuildItem> swaggerUiBuildProducer,
            ShutdownContextBuildItem shutdownContext) {

        WebJarResultsBuildItem.WebJarResult result = webJarResultsBuildItem.byArtifactKey(SWAGGER_UI_WEBJAR_ARTIFACT_KEY);
        if (result == null) {
            return;
        }

        if (shouldInclude(launchMode, swaggerUiConfig)) {
            String swaggerUiPath = nonApplicationRootPathBuildItem.resolvePath(swaggerUiConfig.path());
            swaggerUiBuildProducer.produce(new SwaggerUiBuildItem(result.getFinalDestination(), swaggerUiPath));

            Handler<RoutingContext> handler = recorder.handler(result.getFinalDestination(), swaggerUiPath,
                    result.getWebRootConfigurations(), shutdownContext);

            routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .management("quarkus.smallrye-openapi.management.enabled")
                    .route(swaggerUiConfig.path())
                    .displayOnNotFoundPage("Open API UI")
                    .routeConfigKey("quarkus.swagger-ui.path")
                    .handler(handler)
                    .build());

            routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .management("quarkus.smallrye-openapi.management.enabled")
                    .route(swaggerUiConfig.path() + "*")
                    .handler(handler)
                    .build());
        }
    }

    private byte[] generateIndexHtml(String openApiPath, String swaggerUiPath, SwaggerUiConfig swaggerUiConfig,
            NonApplicationRootPathBuildItem nonApplicationRootPath, LaunchModeBuildItem launchMode,
            DevServicesLauncherConfigResultBuildItem devServicesLauncherConfigResultBuildItem)
            throws IOException {
        Map<Option, String> options = new HashMap<>();
        Map<String, String> urlsMap = null;

        options.put(Option.selfHref, swaggerUiPath);
        if (nonApplicationRootPath != null) {
            options.put(Option.backHref, nonApplicationRootPath.resolvePath(EndpointsProcessor.DEV_UI) + "/");
        } else {
            options.put(Option.backHref, swaggerUiPath);
        }

        // Only add the url if the user did not specify urls
        if (swaggerUiConfig.urls() != null && !swaggerUiConfig.urls().isEmpty()) {
            urlsMap = swaggerUiConfig.urls();
        } else {
            options.put(Option.url, openApiPath);
        }

        if (swaggerUiConfig.title().isPresent()) {
            options.put(Option.title, swaggerUiConfig.title().get());
        } else {
            options.put(Option.title, "OpenAPI UI (Powered by Quarkus " + Version.getVersion() + ")");
        }

        if (swaggerUiConfig.theme().isPresent()) {
            options.put(Option.themeHref, swaggerUiConfig.theme().get().toString());
        }

        if (swaggerUiConfig.footer().isPresent()) {
            options.put(Option.footer, swaggerUiConfig.footer().get());
        }

        if (swaggerUiConfig.deepLinking().isPresent()) {
            options.put(Option.deepLinking, swaggerUiConfig.deepLinking().get().toString());
        }

        if (swaggerUiConfig.displayOperationId().isPresent()) {
            options.put(Option.displayOperationId, swaggerUiConfig.displayOperationId().get().toString());
        }

        if (swaggerUiConfig.defaultModelsExpandDepth().isPresent()) {
            options.put(Option.defaultModelsExpandDepth, String.valueOf(swaggerUiConfig.defaultModelsExpandDepth().getAsInt()));
        }

        if (swaggerUiConfig.defaultModelExpandDepth().isPresent()) {
            options.put(Option.defaultModelExpandDepth, String.valueOf(swaggerUiConfig.defaultModelExpandDepth().getAsInt()));
        }

        if (swaggerUiConfig.defaultModelRendering().isPresent()) {
            options.put(Option.defaultModelRendering, swaggerUiConfig.defaultModelRendering().get());
        }

        if (swaggerUiConfig.displayRequestDuration().isPresent()) {
            options.put(Option.displayRequestDuration, swaggerUiConfig.displayRequestDuration().get().toString());
        }

        if (swaggerUiConfig.docExpansion().isPresent()) {
            options.put(Option.docExpansion, swaggerUiConfig.docExpansion().get().toString());
        }

        if (swaggerUiConfig.filter().isPresent()) {
            options.put(Option.filter, swaggerUiConfig.filter().get());
        }

        if (swaggerUiConfig.maxDisplayedTags().isPresent()) {
            options.put(Option.maxDisplayedTags, String.valueOf(swaggerUiConfig.maxDisplayedTags().getAsInt()));
        }

        if (swaggerUiConfig.operationsSorter().isPresent()) {
            options.put(Option.operationsSorter, swaggerUiConfig.operationsSorter().get());
        }

        if (swaggerUiConfig.showExtensions().isPresent()) {
            options.put(Option.showExtensions, swaggerUiConfig.showExtensions().get().toString());
        }

        if (swaggerUiConfig.showCommonExtensions().isPresent()) {
            options.put(Option.showCommonExtensions, swaggerUiConfig.showCommonExtensions().get().toString());
        }

        if (swaggerUiConfig.tagsSorter().isPresent()) {
            options.put(Option.tagsSorter, swaggerUiConfig.tagsSorter().get());
        }

        if (swaggerUiConfig.onComplete().isPresent()) {
            options.put(Option.onComplete, swaggerUiConfig.onComplete().get());
        }

        if (swaggerUiConfig.syntaxHighlight().isPresent()) {
            options.put(Option.syntaxHighlight, swaggerUiConfig.syntaxHighlight().get());
        }

        if (swaggerUiConfig.oauth2RedirectUrl().isPresent()) {
            options.put(Option.oauth2RedirectUrl, swaggerUiConfig.oauth2RedirectUrl().get());
        } else {
            options.put(Option.oauth2RedirectUrl, swaggerUiPath + "/oauth2-redirect.html");
        }

        if (swaggerUiConfig.requestInterceptor().isPresent()) {
            options.put(Option.requestInterceptor, swaggerUiConfig.requestInterceptor().get());
        }

        if (swaggerUiConfig.requestCurlOptions().isPresent()) {
            String requestCurlOptions = swaggerUiConfig.requestCurlOptions().get().toString();
            options.put(Option.requestCurlOptions, requestCurlOptions);
        }

        if (swaggerUiConfig.responseInterceptor().isPresent()) {
            options.put(Option.responseInterceptor, swaggerUiConfig.responseInterceptor().get());
        }

        if (swaggerUiConfig.showMutatedRequest().isPresent()) {
            options.put(Option.showMutatedRequest, swaggerUiConfig.showMutatedRequest().get().toString());
        }

        if (swaggerUiConfig.supportedSubmitMethods().isPresent()) {
            String httpMethods = swaggerUiConfig.supportedSubmitMethods().get().toString();
            options.put(Option.supportedSubmitMethods, httpMethods);
        }

        if (swaggerUiConfig.validatorUrl().isPresent()) {
            options.put(Option.validatorUrl, swaggerUiConfig.validatorUrl().get());
        }

        if (swaggerUiConfig.withCredentials().isPresent()) {
            options.put(Option.withCredentials, swaggerUiConfig.withCredentials().get().toString());
        }

        if (swaggerUiConfig.modelPropertyMacro().isPresent()) {
            options.put(Option.modelPropertyMacro, swaggerUiConfig.modelPropertyMacro().get());
        }

        if (swaggerUiConfig.parameterMacro().isPresent()) {
            options.put(Option.parameterMacro, swaggerUiConfig.parameterMacro().get());
        }

        if (swaggerUiConfig.persistAuthorization().isPresent()) {
            options.put(Option.persistAuthorization, swaggerUiConfig.persistAuthorization().get().toString());
        } else if (launchMode.getLaunchMode().isDevOrTest()) {
            // In dev mode, default to persist Authorization true
            options.put(Option.persistAuthorization, String.valueOf(true));
        }

        if (swaggerUiConfig.layout().isPresent()) {
            options.put(Option.layout, swaggerUiConfig.layout().get());
        }

        if (swaggerUiConfig.plugins().isPresent()) {
            String plugins = swaggerUiConfig.plugins().get().toString();
            options.put(Option.plugins, plugins);
        }

        if (swaggerUiConfig.scripts().isPresent()) {
            String scripts = String.join(",", swaggerUiConfig.scripts().get());
            options.put(Option.scripts, scripts);
        }

        if (swaggerUiConfig.presets().isPresent()) {
            String presets = swaggerUiConfig.presets().get().toString();
            options.put(Option.presets, presets);
        }

        if (swaggerUiConfig.oauthClientId().isPresent()) {
            String oauthClientId = swaggerUiConfig.oauthClientId().get();
            options.put(Option.oauthClientId, oauthClientId);
        } else if (devServicesLauncherConfigResultBuildItem != null) {
            Map<String, String> devServiceConfig = devServicesLauncherConfigResultBuildItem.getConfig();
            if (devServiceConfig != null && !devServiceConfig.isEmpty()) {
                // Map client Id from OIDC Dev Services
                if (devServiceConfig.containsKey(OIDC_CLIENT_ID)) {
                    String clientId = devServiceConfig.get(OIDC_CLIENT_ID);
                    options.put(Option.oauthClientId, clientId);
                }
            }
        }
        if (swaggerUiConfig.oauthClientSecret().isPresent()) {
            String oauthClientSecret = swaggerUiConfig.oauthClientSecret().get();
            options.put(Option.oauthClientSecret, oauthClientSecret);
        }
        if (swaggerUiConfig.oauthRealm().isPresent()) {
            String oauthRealm = swaggerUiConfig.oauthRealm().get();
            options.put(Option.oauthRealm, oauthRealm);
        }
        if (swaggerUiConfig.oauthAppName().isPresent()) {
            String oauthAppName = swaggerUiConfig.oauthAppName().get();
            options.put(Option.oauthAppName, oauthAppName);
        }
        if (swaggerUiConfig.oauthScopeSeparator().isPresent()) {
            String oauthScopeSeparator = swaggerUiConfig.oauthScopeSeparator().get();
            options.put(Option.oauthScopeSeparator, oauthScopeSeparator);
        }
        if (swaggerUiConfig.oauthScopes().isPresent()) {
            String oauthScopes = swaggerUiConfig.oauthScopes().get();
            options.put(Option.oauthScopes, oauthScopes);
        }
        if (swaggerUiConfig.queryConfigEnabled()) {
            options.put(Option.queryConfigEnabled, "true");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> oauthAdditionalQueryStringParamMap = new HashMap<>();
        if (swaggerUiConfig.oauthAdditionalQueryStringParams().isPresent()) {
            String oauthAdditionalQueryStringParams = swaggerUiConfig.oauthAdditionalQueryStringParams().get();
            Map<String, String> map = objectMapper.readValue(oauthAdditionalQueryStringParams, Map.class);
            if (map == null || map.isEmpty()) {
                LOG.warn(
                        "Property 'quarkus.swagger-ui.oauth-additional-query-string-params' should be a map, example: quarkus.swagger-ui.oauth-additional-query-string-params='{\"foo\": \"bar\"}' ");
            } else {
                oauthAdditionalQueryStringParamMap.putAll(map);
            }
        }

        // If not provided, add generated nonce id. Swagger UI should actually do this. They do not support nonce at the moment. Once they do we can remove this.
        if (!oauthAdditionalQueryStringParamMap.containsKey(OIDC_NONCE_KEY)) {
            oauthAdditionalQueryStringParamMap.put(OIDC_NONCE_KEY, UUID.randomUUID().toString());
        }
        options.put(Option.oauthAdditionalQueryStringParams,
                objectMapper.writeValueAsString(oauthAdditionalQueryStringParamMap));

        if (swaggerUiConfig.oauthUseBasicAuthenticationWithAccessCodeGrant().isPresent()) {
            String oauthUseBasicAuthenticationWithAccessCodeGrant = swaggerUiConfig
                    .oauthUseBasicAuthenticationWithAccessCodeGrant()
                    .get().toString();
            options.put(Option.oauthUseBasicAuthenticationWithAccessCodeGrant, oauthUseBasicAuthenticationWithAccessCodeGrant);
        }
        if (swaggerUiConfig.oauthUsePkceWithAuthorizationCodeGrant().isPresent()) {
            String oauthUsePkceWithAuthorizationCodeGrant = swaggerUiConfig.oauthUsePkceWithAuthorizationCodeGrant().get()
                    .toString();
            options.put(Option.oauthUsePkceWithAuthorizationCodeGrant, oauthUsePkceWithAuthorizationCodeGrant);
        }
        if (swaggerUiConfig.preauthorizeBasicAuthDefinitionKey().isPresent()) {
            String preauthorizeBasicAuthDefinitionKey = swaggerUiConfig.preauthorizeBasicAuthDefinitionKey().get();
            options.put(Option.preauthorizeBasicAuthDefinitionKey, preauthorizeBasicAuthDefinitionKey);
        }
        if (swaggerUiConfig.preauthorizeBasicUsername().isPresent()) {
            String preauthorizeBasicUsername = swaggerUiConfig.preauthorizeBasicUsername().get();
            options.put(Option.preauthorizeBasicUsername, preauthorizeBasicUsername);
        }
        if (swaggerUiConfig.preauthorizeBasicPassword().isPresent()) {
            String preauthorizeBasicPassword = swaggerUiConfig.preauthorizeBasicPassword().get();
            options.put(Option.preauthorizeBasicPassword, preauthorizeBasicPassword);
        }
        if (swaggerUiConfig.preauthorizeApiKeyAuthDefinitionKey().isPresent()) {
            String preauthorizeApiKeyAuthDefinitionKey = swaggerUiConfig.preauthorizeApiKeyAuthDefinitionKey().get();
            options.put(Option.preauthorizeApiKeyAuthDefinitionKey, preauthorizeApiKeyAuthDefinitionKey);
        }
        if (swaggerUiConfig.preauthorizeApiKeyApiKeyValue().isPresent()) {
            String preauthorizeApiKeyApiKeyValue = swaggerUiConfig.preauthorizeApiKeyApiKeyValue().get();
            options.put(Option.preauthorizeApiKeyApiKeyValue, preauthorizeApiKeyApiKeyValue);
        }
        if (swaggerUiConfig.tryItOutEnabled()) {
            options.put(Option.tryItOutEnabled, "true");
        }

        return IndexHtmlCreator.createIndexHtml(urlsMap, swaggerUiConfig.urlsPrimaryName().orElse(null), options);
    }

    private static boolean shouldInclude(LaunchModeBuildItem launchMode, SwaggerUiConfig swaggerUiConfig) {
        return launchMode.getLaunchMode().isDevOrTest() || swaggerUiConfig.alwaysInclude();
    }
}
