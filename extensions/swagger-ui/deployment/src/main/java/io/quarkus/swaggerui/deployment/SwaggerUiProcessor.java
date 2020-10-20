package io.quarkus.swaggerui.deployment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import javax.inject.Inject;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.builder.Version;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.WebJarUtil;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.swaggerui.runtime.SwaggerUiRecorder;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.smallrye.openapi.ui.DocExpansion;
import io.smallrye.openapi.ui.HttpMethod;
import io.smallrye.openapi.ui.IndexCreator;
import io.smallrye.openapi.ui.Option;
import io.smallrye.openapi.ui.ThemeHref;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class SwaggerUiProcessor {

    private static final String SWAGGER_UI_WEBJAR_GROUP_ID = "io.smallrye";
    private static final String SWAGGER_UI_WEBJAR_ARTIFACT_ID = "smallrye-open-api-ui";
    private static final String SWAGGER_UI_WEBJAR_PREFIX = "META-INF/resources/openapi-ui/";
    private static final String SWAGGER_UI_FINAL_DESTINATION = "META-INF/swagger-ui-files";

    /**
     * The configuration for Swagger UI.
     */
    SwaggerUiConfig swaggerUiConfig;

    SmallRyeOpenApiConfig openapi;

    @Inject
    private LaunchModeBuildItem launch;

    @BuildStep
    void feature(BuildProducer<FeatureBuildItem> feature) {
        if (swaggerUiConfig.enable && (launch.getLaunchMode().isDevOrTest() || swaggerUiConfig.alwaysInclude)) {
            feature.produce(new FeatureBuildItem(Feature.SWAGGER_UI));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void registerSwaggerUiServletExtension(SwaggerUiRecorder recorder,
            BuildProducer<RouteBuildItem> routes,
            BeanContainerBuildItem container,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceBuildItemBuildProducer,
            HttpRootPathBuildItem httpRootPathBuildItem,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            CurateOutcomeBuildItem curateOutcomeBuildItem) throws Exception {

        if ("/".equals(swaggerUiConfig.path)) {
            throw new ConfigurationError(
                    "quarkus.swagger-ui.path was set to \"/\", this is not allowed as it blocks the application from serving anything else.");
        }

        if (!swaggerUiConfig.enable) {
            return;
        }

        String openApiPath = httpRootPathBuildItem.adjustPath(openapi.path);
        AppArtifact artifact = WebJarUtil.getAppArtifact(curateOutcomeBuildItem, SWAGGER_UI_WEBJAR_GROUP_ID,
                SWAGGER_UI_WEBJAR_ARTIFACT_ID);

        if (launch.getLaunchMode().isDevOrTest()) {
            Path tempPath = WebJarUtil.devOrTest(curateOutcomeBuildItem, launch, artifact, SWAGGER_UI_WEBJAR_PREFIX);
            // Update index.html
            WebJarUtil.updateFile(tempPath.resolve("index.html"), generateIndexHtml(openApiPath));

            Handler<RoutingContext> handler = recorder.handler(tempPath.toAbsolutePath().toString(),
                    httpRootPathBuildItem.adjustPath(swaggerUiConfig.path));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path, handler));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path + "/*", handler));
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(swaggerUiConfig.path + "/"));
        } else if (swaggerUiConfig.alwaysInclude) {
            Map<String, byte[]> files = WebJarUtil.production(curateOutcomeBuildItem, artifact, SWAGGER_UI_WEBJAR_PREFIX);
            ThemeHref theme = swaggerUiConfig.theme.orElse(ThemeHref.feeling_blue);
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                String fileName = file.getKey();
                // Make sure to only include the selected theme
                if (fileName.equals(theme.toString()) || !fileName.startsWith("theme-")) {
                    byte[] content;
                    if (fileName.endsWith("index.html")) {
                        content = generateIndexHtml(openApiPath);
                    } else {
                        content = file.getValue();
                    }
                    fileName = SWAGGER_UI_FINAL_DESTINATION + "/" + fileName;
                    generatedResources.produce(new GeneratedResourceBuildItem(fileName, content));
                    nativeImageResourceBuildItemBuildProducer.produce(new NativeImageResourceBuildItem(fileName));
                }
            }

            Handler<RoutingContext> handler = recorder
                    .handler(SWAGGER_UI_FINAL_DESTINATION, httpRootPathBuildItem.adjustPath(swaggerUiConfig.path));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path, handler));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path + "/*", handler));
        }
    }

    private byte[] generateIndexHtml(String openApiPath) throws IOException {
        Map<Option, String> options = new HashMap<>();
        Map<String, String> urlsMap = null;

        options.put(Option.selfHref, swaggerUiConfig.path);

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

        return IndexCreator.createIndexHtml(urlsMap, swaggerUiConfig.urlsPrimaryName.orElse(null), options);
    }

    @ConfigRoot
    static final class SwaggerUiConfig {
        /**
         * The path where Swagger UI is available.
         * <p>
         * The value `/` is not allowed as it blocks the application from serving anything else.
         */
        @ConfigItem(defaultValue = "/swagger-ui")
        String path;

        /**
         * If this should be included every time. By default this is only included when the application is running
         * in dev mode.
         */
        @ConfigItem
        boolean alwaysInclude;

        /**
         * If Swagger UI should be enabled. By default, Swagger UI is enabled.
         */
        @ConfigItem(defaultValue = "true")
        boolean enable;

        /**
         * The urls that will be included as options. By default the OpenAPI path will be used.
         * Here you can override that and supply multiple urls that will appear in the TopBar plugin.
         */
        @ConfigItem
        Map<String, String> urls;

        /**
         * If urls option is used, this will be the name of the default selection.
         */
        @ConfigItem
        Optional<String> urlsPrimaryName;

        /**
         * The html title for the page.
         */
        @ConfigItem
        Optional<String> title;

        /**
         * Swagger UI theme to be used.
         */
        @ConfigItem
        Optional<ThemeHref> theme;

        /**
         * A footer for the html page. Nothing by default.
         */
        @ConfigItem
        Optional<String> footer;

        /**
         * If set to true, enables deep linking for tags and operations.
         */
        @ConfigItem
        Optional<Boolean> deepLinking;

        /**
         * Controls the display of operationId in operations list. The default is false.
         */
        @ConfigItem
        Optional<Boolean> displayOperationId;

        /**
         * The default expansion depth for models (set to -1 completely hide the models).
         */
        @ConfigItem
        OptionalInt defaultModelsExpandDepth;

        /**
         * The default expansion depth for the model on the model-example section.
         */
        @ConfigItem
        OptionalInt defaultModelExpandDepth;

        /**
         * Controls how the model is shown when the API is first rendered.
         */
        @ConfigItem
        Optional<String> defaultModelRendering;

        /**
         * Controls the display of the request duration (in milliseconds) for "Try it out" requests.
         */
        @ConfigItem
        Optional<Boolean> displayRequestDuration;

        /**
         * Controls the default expansion setting for the operations and tags.
         */
        @ConfigItem
        Optional<DocExpansion> docExpansion;

        /**
         * If set, enables filtering. The top bar will show an edit box that you can use to filter the tagged operations that
         * are shown.
         * Can be Boolean to enable or disable, or a string, in which case filtering will be enabled using that string as the
         * filter expression.
         * Filtering is case sensitive matching the filter expression anywhere inside the tag.
         */
        @ConfigItem
        Optional<String> filter;

        /**
         * If set, limits the number of tagged operations displayed to at most this many. The default is to show all operations.
         */
        @ConfigItem
        OptionalInt maxDisplayedTags;

        /**
         * Apply a sort to the operation list of each API.
         * It can be 'alpha' (sort by paths alphanumerically), 'method' (sort by HTTP method) or a function (see
         * Array.prototype.sort() to know how sort function works).
         * Default is the order returned by the server unchanged.
         */
        @ConfigItem
        Optional<String> operationsSorter;

        /**
         * Controls the display of vendor extension (x-) fields and values for Operations, Parameters, and Schema.
         */
        @ConfigItem
        Optional<Boolean> showExtensions;

        /**
         * Controls the display of extensions (pattern, maxLength, minLength, maximum, minimum) fields and values for
         * Parameters.
         */
        @ConfigItem
        Optional<Boolean> showCommonExtensions;

        /**
         * Apply a sort to the tag list of each API.
         * It can be 'alpha' (sort by paths alphanumerically) or a function (see Array.prototype.sort() to learn how to write a
         * sort function).
         * Two tag name strings are passed to the sorter for each pass. Default is the order determined by Swagger UI.
         */
        @ConfigItem
        Optional<String> tagsSorter;

        /**
         * Provides a mechanism to be notified when Swagger UI has finished rendering a newly provided definition.
         */
        @ConfigItem
        Optional<String> onComplete;

        /**
         * Set to false to deactivate syntax highlighting of payloads and cURL command, can be otherwise an object with the
         * activate and theme properties.
         */
        @ConfigItem
        Optional<String> syntaxHighlight;

        /**
         * OAuth redirect URL.
         */
        @ConfigItem
        Optional<String> oauth2RedirectUrl;

        /**
         * MUST be a function. Function to intercept remote definition, "Try it out", and OAuth 2.0 requests.
         * Accepts one argument requestInterceptor(request) and must return the modified request, or a Promise that resolves to
         * the modified request.
         */
        @ConfigItem
        Optional<String> requestInterceptor;

        /**
         * If set, MUST be an array of command line options available to the curl command.
         * This can be set on the mutated request in the requestInterceptor function.
         */
        @ConfigItem
        Optional<List<String>> requestCurlOptions;

        /**
         * MUST be a function. Function to intercept remote definition, "Try it out", and OAuth 2.0 responses.
         * Accepts one argument responseInterceptor(response) and must return the modified response, or a Promise that resolves
         * to the modified response.
         */
        @ConfigItem
        Optional<String> responseInterceptor;

        /**
         * If set to true, uses the mutated request returned from a requestInterceptor to produce the curl command in the UI,
         * otherwise the request before the requestInterceptor was applied is used.
         */
        @ConfigItem
        Optional<Boolean> showMutatedRequest;

        /**
         * List of HTTP methods that have the "Try it out" feature enabled.
         * An empty array disables "Try it out" for all operations. This does not filter the operations from the display.
         */
        @ConfigItem
        Optional<List<HttpMethod>> supportedSubmitMethods;

        /**
         * By default, Swagger UI attempts to validate specs against swagger.io's online validator.
         * You can use this parameter to set a different validator URL, for example for locally deployed validators (Validator
         * Badge).
         * Setting it to either none, 127.0.0.1 or localhost will disable validation.
         */
        @ConfigItem
        Optional<String> validatorUrl;

        /**
         * If set to true, enables passing credentials, as defined in the Fetch standard, in CORS requests that are sent by the
         * browser.
         */
        @ConfigItem
        Optional<Boolean> withCredentials;

        /**
         * Function to set default values to each property in model. Accepts one argument modelPropertyMacro(property), property
         * is immutable
         */
        @ConfigItem
        Optional<String> modelPropertyMacro;

        /**
         * Function to set default value to parameters. Accepts two arguments parameterMacro(operation, parameter).
         * Operation and parameter are objects passed for context, both remain immutable
         */
        @ConfigItem
        Optional<String> parameterMacro;

        /**
         * If set to true, it persists authorization data and it would not be lost on browser close/refresh
         */
        @ConfigItem
        Optional<Boolean> persistAuthorization;

        /**
         * The name of a component available via the plugin system to use as the top-level layout for Swagger UI.
         */
        @ConfigItem
        Optional<String> layout;

        /**
         * A list of plugin functions to use in Swagger UI.
         */
        @ConfigItem
        Optional<List<String>> plugins;

        /**
         * A list of presets to use in Swagger UI.
         */
        @ConfigItem
        Optional<List<String>> presets;
    }
}
