package io.quarkus.swaggerui.deployment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.WebJarUtil;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.swaggerui.runtime.SwaggerUiRecorder;
import io.quarkus.swaggerui.runtime.SwaggerUiRuntimeConfig;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.smallrye.openapi.ui.IndexHtmlCreator;
import io.smallrye.openapi.ui.Option;
import io.smallrye.openapi.ui.ThemeHref;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class SwaggerUiProcessor {

    private static final String SWAGGER_UI_WEBJAR_GROUP_ID = "io.smallrye";
    private static final String SWAGGER_UI_WEBJAR_ARTIFACT_ID = "smallrye-open-api-ui";
    private static final String SWAGGER_UI_WEBJAR_PREFIX = "META-INF/resources/openapi-ui/";
    private static final String SWAGGER_UI_FINAL_DESTINATION = "META-INF/swagger-ui-files";

    // Branding files to monitor for changes
    private static final String BRANDING_DIR = "META-INF/branding/";
    private static final String BRANDING_LOGO_GENERAL = BRANDING_DIR + "logo.png";
    private static final String BRANDING_LOGO_MODULE = BRANDING_DIR + "smallrye-open-api-ui.png";
    private static final String BRANDING_STYLE_GENERAL = BRANDING_DIR + "style.css";
    private static final String BRANDING_STYLE_MODULE = BRANDING_DIR + "smallrye-open-api-ui.css";
    private static final String BRANDING_FAVICON_GENERAL = BRANDING_DIR + "favicon.ico";
    private static final String BRANDING_FAVICON_MODULE = BRANDING_DIR + "smallrye-open-api-ui.ico";

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
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceBuildItemBuildProducer,
            BuildProducer<SwaggerUiBuildItem> swaggerUiBuildProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            LaunchModeBuildItem launchMode,
            SwaggerUiConfig swaggerUiConfig,
            SmallRyeOpenApiConfig openapi,
            LiveReloadBuildItem liveReloadBuildItem) throws Exception {

        if (shouldInclude(launchMode, swaggerUiConfig)) {
            if ("/".equals(swaggerUiConfig.path)) {
                throw new ConfigurationError(
                        "quarkus.swagger-ui.path was set to \"/\", this is not allowed as it blocks the application from serving anything else.");
            }

            if (openapi.path.equalsIgnoreCase(swaggerUiConfig.path)) {
                throw new ConfigurationError(
                        "quarkus.smallrye-openapi.path and quarkus.swagger-ui.path was set to the same value, this is not allowed as the paths needs to be unique ["
                                + openapi.path + "].");

            }

            String openApiPath = nonApplicationRootPathBuildItem.resolvePath(openapi.path);
            String swaggerUiPath = nonApplicationRootPathBuildItem.resolvePath(swaggerUiConfig.path);

            AppArtifact artifact = WebJarUtil.getAppArtifact(curateOutcomeBuildItem, SWAGGER_UI_WEBJAR_GROUP_ID,
                    SWAGGER_UI_WEBJAR_ARTIFACT_ID);

            if (launchMode.getLaunchMode().isDevOrTest()) {
                Path tempPath = WebJarUtil.copyResourcesForDevOrTest(liveReloadBuildItem, curateOutcomeBuildItem, launchMode,
                        artifact,
                        SWAGGER_UI_WEBJAR_PREFIX);
                // Update index.html
                WebJarUtil.updateFile(tempPath.resolve("index.html"),
                        generateIndexHtml(openApiPath, swaggerUiPath, swaggerUiConfig));

                swaggerUiBuildProducer.produce(new SwaggerUiBuildItem(tempPath.toAbsolutePath().toString(), swaggerUiPath));

                // Handle live reload of branding files
                if (liveReloadBuildItem.isLiveReload() && !liveReloadBuildItem.getChangedResources().isEmpty()) {
                    WebJarUtil.hotReloadBrandingChanges(curateOutcomeBuildItem, launchMode, artifact,
                            liveReloadBuildItem.getChangedResources());
                }
            } else {
                Map<String, byte[]> files = WebJarUtil.copyResourcesForProduction(curateOutcomeBuildItem, artifact,
                        SWAGGER_UI_WEBJAR_PREFIX);
                ThemeHref theme = swaggerUiConfig.theme.orElse(ThemeHref.feeling_blue);
                for (Map.Entry<String, byte[]> file : files.entrySet()) {
                    String fileName = file.getKey();
                    // Make sure to only include the selected theme
                    if (fileName.equals(theme.toString()) || !fileName.startsWith("theme-")) {
                        byte[] content;
                        if (fileName.endsWith("index.html")) {
                            content = generateIndexHtml(openApiPath, swaggerUiPath, swaggerUiConfig);
                        } else {
                            content = file.getValue();
                        }
                        fileName = SWAGGER_UI_FINAL_DESTINATION + "/" + fileName;
                        generatedResources.produce(new GeneratedResourceBuildItem(fileName, content));
                        nativeImageResourceBuildItemBuildProducer.produce(new NativeImageResourceBuildItem(fileName));
                    }
                }
                swaggerUiBuildProducer.produce(new SwaggerUiBuildItem(SWAGGER_UI_FINAL_DESTINATION, swaggerUiPath));
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void registerSwaggerUiHandler(SwaggerUiRecorder recorder,
            BuildProducer<RouteBuildItem> routes,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            SwaggerUiBuildItem finalDestinationBuildItem,
            SwaggerUiRuntimeConfig runtimeConfig,
            LaunchModeBuildItem launchMode,
            SwaggerUiConfig swaggerUiConfig) throws Exception {

        if (shouldInclude(launchMode, swaggerUiConfig)) {
            Handler<RoutingContext> handler = recorder.handler(finalDestinationBuildItem.getSwaggerUiFinalDestination(),
                    finalDestinationBuildItem.getSwaggerUiPath(),
                    runtimeConfig);

            routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .route(swaggerUiConfig.path)
                    .displayOnNotFoundPage("Open API UI")
                    .routeConfigKey("quarkus.swagger-ui.path")
                    .handler(handler)
                    .build());

            routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .route(swaggerUiConfig.path + "*")
                    .handler(handler)
                    .build());
        }
    }

    private byte[] generateIndexHtml(String openApiPath, String swaggerUiPath, SwaggerUiConfig swaggerUiConfig)
            throws IOException {
        Map<Option, String> options = new HashMap<>();
        Map<String, String> urlsMap = null;

        options.put(Option.selfHref, swaggerUiPath);

        // Only add the url if the user did not specified urls
        if (swaggerUiConfig.urls != null && !swaggerUiConfig.urls.isEmpty()) {
            urlsMap = swaggerUiConfig.urls;
        } else {
            options.put(Option.url, openApiPath);
        }

        if (swaggerUiConfig.title.isPresent()) {
            options.put(Option.title, swaggerUiConfig.title.get());
        } else {
            options.put(Option.title, "OpenAPI UI (Powered by Quarkus " + Version.getVersion() + ")");
        }

        if (swaggerUiConfig.theme.isPresent()) {
            options.put(Option.themeHref, swaggerUiConfig.theme.get().toString());
        }

        if (swaggerUiConfig.footer.isPresent()) {
            options.put(Option.footer, swaggerUiConfig.footer.get());
        }

        if (swaggerUiConfig.deepLinking.isPresent()) {
            options.put(Option.deepLinking, swaggerUiConfig.deepLinking.get().toString());
        }

        if (swaggerUiConfig.displayOperationId.isPresent()) {
            options.put(Option.displayOperationId, swaggerUiConfig.displayOperationId.get().toString());
        }

        if (swaggerUiConfig.defaultModelsExpandDepth.isPresent()) {
            options.put(Option.defaultModelsExpandDepth, String.valueOf(swaggerUiConfig.defaultModelsExpandDepth.getAsInt()));
        }

        if (swaggerUiConfig.defaultModelExpandDepth.isPresent()) {
            options.put(Option.defaultModelExpandDepth, String.valueOf(swaggerUiConfig.defaultModelExpandDepth.getAsInt()));
        }

        if (swaggerUiConfig.defaultModelRendering.isPresent()) {
            options.put(Option.defaultModelRendering, swaggerUiConfig.defaultModelRendering.get());
        }

        if (swaggerUiConfig.displayRequestDuration.isPresent()) {
            options.put(Option.displayRequestDuration, swaggerUiConfig.displayRequestDuration.get().toString());
        }

        if (swaggerUiConfig.docExpansion.isPresent()) {
            options.put(Option.docExpansion, swaggerUiConfig.docExpansion.get().toString());
        }

        if (swaggerUiConfig.filter.isPresent()) {
            options.put(Option.filter, swaggerUiConfig.filter.get());
        }

        if (swaggerUiConfig.maxDisplayedTags.isPresent()) {
            options.put(Option.maxDisplayedTags, String.valueOf(swaggerUiConfig.maxDisplayedTags.getAsInt()));
        }

        if (swaggerUiConfig.operationsSorter.isPresent()) {
            options.put(Option.operationsSorter, swaggerUiConfig.operationsSorter.get());
        }

        if (swaggerUiConfig.showExtensions.isPresent()) {
            options.put(Option.showExtensions, swaggerUiConfig.showExtensions.get().toString());
        }

        if (swaggerUiConfig.showCommonExtensions.isPresent()) {
            options.put(Option.showCommonExtensions, swaggerUiConfig.showCommonExtensions.get().toString());
        }

        if (swaggerUiConfig.tagsSorter.isPresent()) {
            options.put(Option.tagsSorter, swaggerUiConfig.tagsSorter.get());
        }

        if (swaggerUiConfig.onComplete.isPresent()) {
            options.put(Option.onComplete, swaggerUiConfig.onComplete.get());
        }

        if (swaggerUiConfig.syntaxHighlight.isPresent()) {
            options.put(Option.syntaxHighlight, swaggerUiConfig.syntaxHighlight.get());
        }

        if (swaggerUiConfig.oauth2RedirectUrl.isPresent()) {
            options.put(Option.oauth2RedirectUrl, swaggerUiConfig.oauth2RedirectUrl.get());
        }

        if (swaggerUiConfig.requestInterceptor.isPresent()) {
            options.put(Option.requestInterceptor, swaggerUiConfig.requestInterceptor.get());
        }

        if (swaggerUiConfig.requestCurlOptions.isPresent()) {
            String requestCurlOptions = swaggerUiConfig.requestCurlOptions.get().toString();
            options.put(Option.requestCurlOptions, requestCurlOptions);
        }

        if (swaggerUiConfig.responseInterceptor.isPresent()) {
            options.put(Option.responseInterceptor, swaggerUiConfig.responseInterceptor.get());
        }

        if (swaggerUiConfig.showMutatedRequest.isPresent()) {
            options.put(Option.showMutatedRequest, swaggerUiConfig.showMutatedRequest.get().toString());
        }

        if (swaggerUiConfig.supportedSubmitMethods.isPresent()) {
            String httpMethods = swaggerUiConfig.supportedSubmitMethods.get().toString();
            options.put(Option.supportedSubmitMethods, httpMethods);
        }

        if (swaggerUiConfig.validatorUrl.isPresent()) {
            options.put(Option.validatorUrl, swaggerUiConfig.validatorUrl.get());
        }

        if (swaggerUiConfig.withCredentials.isPresent()) {
            options.put(Option.withCredentials, swaggerUiConfig.withCredentials.get().toString());
        }

        if (swaggerUiConfig.modelPropertyMacro.isPresent()) {
            options.put(Option.modelPropertyMacro, swaggerUiConfig.modelPropertyMacro.get());
        }

        if (swaggerUiConfig.parameterMacro.isPresent()) {
            options.put(Option.parameterMacro, swaggerUiConfig.parameterMacro.get());
        }

        if (swaggerUiConfig.persistAuthorization.isPresent()) {
            options.put(Option.persistAuthorization, swaggerUiConfig.persistAuthorization.get().toString());
        }

        if (swaggerUiConfig.layout.isPresent()) {
            options.put(Option.layout, swaggerUiConfig.layout.get());
        }

        if (swaggerUiConfig.plugins.isPresent()) {
            String plugins = swaggerUiConfig.plugins.get().toString();
            options.put(Option.plugins, plugins);
        }

        if (swaggerUiConfig.presets.isPresent()) {
            String presets = swaggerUiConfig.presets.get().toString();
            options.put(Option.presets, presets);
        }

        if (swaggerUiConfig.oauthClientId.isPresent()) {
            String oauthClientId = swaggerUiConfig.oauthClientId.get();
            options.put(Option.oauthClientId, oauthClientId);
        }
        if (swaggerUiConfig.oauthClientSecret.isPresent()) {
            String oauthClientSecret = swaggerUiConfig.oauthClientSecret.get();
            options.put(Option.oauthClientSecret, oauthClientSecret);
        }
        if (swaggerUiConfig.oauthRealm.isPresent()) {
            String oauthRealm = swaggerUiConfig.oauthRealm.get();
            options.put(Option.oauthRealm, oauthRealm);
        }
        if (swaggerUiConfig.oauthAppName.isPresent()) {
            String oauthAppName = swaggerUiConfig.oauthAppName.get();
            options.put(Option.oauthAppName, oauthAppName);
        }
        if (swaggerUiConfig.oauthScopeSeparator.isPresent()) {
            String oauthScopeSeparator = swaggerUiConfig.oauthScopeSeparator.get();
            options.put(Option.oauthScopeSeparator, oauthScopeSeparator);
        }
        if (swaggerUiConfig.oauthScopes.isPresent()) {
            String oauthScopes = swaggerUiConfig.oauthScopes.get();
            options.put(Option.oauthScopes, oauthScopes);
        }
        if (swaggerUiConfig.oauthAdditionalQueryStringParams.isPresent()) {
            String oauthAdditionalQueryStringParams = swaggerUiConfig.oauthAdditionalQueryStringParams.get();
            options.put(Option.oauthAdditionalQueryStringParams, oauthAdditionalQueryStringParams);
        }
        if (swaggerUiConfig.oauthUseBasicAuthenticationWithAccessCodeGrant.isPresent()) {
            String oauthUseBasicAuthenticationWithAccessCodeGrant = swaggerUiConfig.oauthUseBasicAuthenticationWithAccessCodeGrant
                    .get().toString();
            options.put(Option.oauthUseBasicAuthenticationWithAccessCodeGrant, oauthUseBasicAuthenticationWithAccessCodeGrant);
        }
        if (swaggerUiConfig.oauthUsePkceWithAuthorizationCodeGrant.isPresent()) {
            String oauthUsePkceWithAuthorizationCodeGrant = swaggerUiConfig.oauthUsePkceWithAuthorizationCodeGrant.get()
                    .toString();
            options.put(Option.oauthUsePkceWithAuthorizationCodeGrant, oauthUsePkceWithAuthorizationCodeGrant);
        }
        if (swaggerUiConfig.preauthorizeBasicAuthDefinitionKey.isPresent()) {
            String preauthorizeBasicAuthDefinitionKey = swaggerUiConfig.preauthorizeBasicAuthDefinitionKey.get();
            options.put(Option.preauthorizeBasicAuthDefinitionKey, preauthorizeBasicAuthDefinitionKey);
        }
        if (swaggerUiConfig.preauthorizeBasicUsername.isPresent()) {
            String preauthorizeBasicUsername = swaggerUiConfig.preauthorizeBasicUsername.get();
            options.put(Option.preauthorizeBasicUsername, preauthorizeBasicUsername);
        }
        if (swaggerUiConfig.preauthorizeBasicPassword.isPresent()) {
            String preauthorizeBasicPassword = swaggerUiConfig.preauthorizeBasicPassword.get();
            options.put(Option.preauthorizeBasicPassword, preauthorizeBasicPassword);
        }
        if (swaggerUiConfig.preauthorizeApiKeyAuthDefinitionKey.isPresent()) {
            String preauthorizeApiKeyAuthDefinitionKey = swaggerUiConfig.preauthorizeApiKeyAuthDefinitionKey.get();
            options.put(Option.preauthorizeApiKeyAuthDefinitionKey, preauthorizeApiKeyAuthDefinitionKey);
        }
        if (swaggerUiConfig.preauthorizeApiKeyApiKeyValue.isPresent()) {
            String preauthorizeApiKeyApiKeyValue = swaggerUiConfig.preauthorizeApiKeyApiKeyValue.get();
            options.put(Option.preauthorizeApiKeyApiKeyValue, preauthorizeApiKeyApiKeyValue);
        }

        return IndexHtmlCreator.createIndexHtml(urlsMap, swaggerUiConfig.urlsPrimaryName.orElse(null), options);
    }

    private static boolean shouldInclude(LaunchModeBuildItem launchMode, SwaggerUiConfig swaggerUiConfig) {
        return launchMode.getLaunchMode().isDevOrTest() || swaggerUiConfig.alwaysInclude;
    }
}
