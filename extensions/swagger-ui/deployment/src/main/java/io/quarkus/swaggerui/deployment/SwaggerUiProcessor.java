package io.quarkus.swaggerui.deployment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.bootstrap.model.AppArtifact;
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
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class SwaggerUiProcessor {

    private static final Logger log = Logger.getLogger(SwaggerUiProcessor.class.getName());

    private static final String SWAGGER_UI_WEBJAR_GROUP_ID = "io.smallrye";
    private static final String SWAGGER_UI_WEBJAR_ARTIFACT_ID = "smallrye-open-api-ui";
    private static final String SWAGGER_UI_WEBJAR_PREFIX = "META-INF/resources/openapi-ui/";
    private static final String SWAGGER_UI_FINAL_DESTINATION = "META-INF/swagger-ui-files";
    private static final String LINE_TO_UPDATE = "const documentpath = '";
    private static final String LINE_FORMAT = LINE_TO_UPDATE + "%s';";

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
            WebJarUtil.updateUrl(tempPath.resolve("index.html"), openApiPath, LINE_TO_UPDATE, LINE_FORMAT);

            Handler<RoutingContext> handler = recorder.handler(tempPath.toAbsolutePath().toString(),
                    httpRootPathBuildItem.adjustPath(swaggerUiConfig.path));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path, handler));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path + "/*", handler));
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(swaggerUiConfig.path + "/"));
        } else if (swaggerUiConfig.alwaysInclude) {
            Map<String, byte[]> files = WebJarUtil.production(curateOutcomeBuildItem, artifact, SWAGGER_UI_WEBJAR_PREFIX);

            for (Map.Entry<String, byte[]> file : files.entrySet()) {

                String fileName = file.getKey();
                byte[] content = file.getValue();
                if (fileName.endsWith("index.html")) {
                    content = WebJarUtil
                            .updateUrl(new String(content, StandardCharsets.UTF_8), openApiPath, LINE_TO_UPDATE, LINE_FORMAT)
                            .getBytes(StandardCharsets.UTF_8);
                }
                fileName = SWAGGER_UI_FINAL_DESTINATION + "/" + fileName;
                generatedResources.produce(new GeneratedResourceBuildItem(fileName, content));
                nativeImageResourceBuildItemBuildProducer.produce(new NativeImageResourceBuildItem(fileName));
            }

            Handler<RoutingContext> handler = recorder
                    .handler(SWAGGER_UI_FINAL_DESTINATION, httpRootPathBuildItem.adjustPath(swaggerUiConfig.path));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path, handler));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path + "/*", handler));
        }
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
    }
}
