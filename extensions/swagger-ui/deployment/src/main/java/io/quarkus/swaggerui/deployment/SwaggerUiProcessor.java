package io.quarkus.swaggerui.deployment;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.index.ClassPathArtifactResolver;
import io.quarkus.deployment.index.ResolvedArtifact;
import io.quarkus.deployment.util.FileUtil;
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

    private static final String SWAGGER_UI_WEBJAR_GROUP_ID = "org.webjars";
    private static final String SWAGGER_UI_WEBJAR_ARTIFACT_ID = "swagger-ui";
    private static final String SWAGGER_UI_WEBJAR_PREFIX = "META-INF/resources/webjars/swagger-ui";
    private static final String SWAGGER_UI_FINAL_DESTINATION = "META-INF/swagger-ui-files";
    private static final Pattern SWAGGER_UI_DEFAULT_API_URL_PATTERN = Pattern.compile("(.* url: \")(.*)(\",.*)",
            Pattern.DOTALL);
    private static final String TEMP_DIR_PREFIX = "quarkus-swagger-ui_" + System.nanoTime();

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
            feature.produce(new FeatureBuildItem(FeatureBuildItem.SWAGGER_UI));
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void registerSwaggerUiServletExtension(SwaggerUiRecorder recorder,
            BuildProducer<RouteBuildItem> routes,
            BeanContainerBuildItem container,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceBuildItemBuildProducer,
            LiveReloadBuildItem liveReloadBuildItem,
            HttpRootPathBuildItem httpRootPathBuildItem,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints) throws Exception {

        if ("/".equals(swaggerUiConfig.path)) {
            throw new ConfigurationError(
                    "quarkus.swagger-ui.path was set to \"/\", this is not allowed as it blocks the application from serving anything else.");
        }

        if (!swaggerUiConfig.enable) {
            return;
        }

        String openApiPath = httpRootPathBuildItem.adjustPath(openapi.path);
        if (launch.getLaunchMode().isDevOrTest()) {
            CachedSwaggerUI cached = liveReloadBuildItem.getContextObject(CachedSwaggerUI.class);

            boolean extractionNeeded = cached == null;
            if (cached != null && !cached.cachedOpenAPIPath.equals(openApiPath)) {
                try {
                    FileUtil.deleteDirectory(Paths.get(cached.cachedDirectory));
                } catch (IOException e) {
                    log.error("Failed to clean Swagger UI temp directory on restart", e);
                }
                extractionNeeded = true;
            }
            if (extractionNeeded) {
                if (cached == null) {
                    cached = new CachedSwaggerUI();
                    liveReloadBuildItem.setContextObject(CachedSwaggerUI.class, cached);
                    Runtime.getRuntime().addShutdownHook(new Thread(cached, "Swagger UI Shutdown Hook"));
                }
                try {
                    ResolvedArtifact artifact = getSwaggerUiArtifact();
                    Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX).toRealPath();
                    extractSwaggerUi(artifact, tempDir);
                    updateApiUrl(tempDir.resolve("index.html"), openApiPath);
                    cached.cachedDirectory = tempDir.toAbsolutePath().toString();
                    cached.cachedOpenAPIPath = openApiPath;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            Handler<RoutingContext> handler = recorder.handler(cached.cachedDirectory,
                    httpRootPathBuildItem.adjustPath(swaggerUiConfig.path));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path, handler));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path + "/*", handler));
            displayableEndpoints.produce(new NotFoundPageDisplayableEndpointBuildItem(swaggerUiConfig.path + "/"));
        } else if (swaggerUiConfig.alwaysInclude) {
            ResolvedArtifact artifact = getSwaggerUiArtifact();
            //we are including in a production artifact
            //just stick the files in the generated output
            //we could do this for dev mode as well but then we need to extract them every time
            File artifactFile = artifact.getArtifactPath().toFile();
            try (JarFile jarFile = new JarFile(artifactFile)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                String versionedSwaggerUiWebjarPrefix = format("%s/%s/", SWAGGER_UI_WEBJAR_PREFIX, artifact.getVersion());
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(versionedSwaggerUiWebjarPrefix) && !entry.isDirectory()) {
                        try (InputStream inputStream = jarFile.getInputStream(entry)) {
                            String filename = entry.getName().replace(versionedSwaggerUiWebjarPrefix, "");
                            byte[] content = FileUtil.readFileContents(inputStream);
                            if (entry.getName().endsWith("index.html")) {
                                content = updateApiUrl(new String(content, StandardCharsets.UTF_8), openApiPath)
                                        .getBytes(StandardCharsets.UTF_8);
                            }
                            String fileName = SWAGGER_UI_FINAL_DESTINATION + "/" + filename;
                            generatedResources
                                    .produce(new GeneratedResourceBuildItem(fileName,
                                            content));
                            nativeImageResourceBuildItemBuildProducer.produce(new NativeImageResourceBuildItem(fileName));

                        }
                    }
                }
            }

            Handler<RoutingContext> handler = recorder
                    .handler(SWAGGER_UI_FINAL_DESTINATION, httpRootPathBuildItem.adjustPath(swaggerUiConfig.path));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path, handler));
            routes.produce(new RouteBuildItem(swaggerUiConfig.path + "/*", handler));
        }
    }

    private ResolvedArtifact getSwaggerUiArtifact() {
        ClassPathArtifactResolver resolver = new ClassPathArtifactResolver(SwaggerUiProcessor.class.getClassLoader());
        return resolver.getArtifact(SWAGGER_UI_WEBJAR_GROUP_ID, SWAGGER_UI_WEBJAR_ARTIFACT_ID, null);
    }

    private void extractSwaggerUi(ResolvedArtifact artifact, Path resourceDir) throws IOException {
        File artifactFile = artifact.getArtifactPath().toFile();
        try (JarFile jarFile = new JarFile(artifactFile)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            String versionedSwaggerUiWebjarPrefix = format("%s/%s/", SWAGGER_UI_WEBJAR_PREFIX, artifact.getVersion());
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(versionedSwaggerUiWebjarPrefix) && !entry.isDirectory()) {
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        String filename = entry.getName().replace(versionedSwaggerUiWebjarPrefix, "");
                        Files.copy(inputStream, resourceDir.resolve(filename));
                    }
                }
            }
        }
    }

    private void updateApiUrl(Path indexHtml, String openApiPath) throws IOException {
        String content = new String(Files.readAllBytes(indexHtml), StandardCharsets.UTF_8);
        String result = updateApiUrl(content, openApiPath);
        if (result != null) {
            Files.write(indexHtml, result.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String updateApiUrl(String original, String openApiPath) {

        Matcher uriMatcher = SWAGGER_UI_DEFAULT_API_URL_PATTERN.matcher(original);
        if (uriMatcher.matches()) {
            return uriMatcher.replaceFirst("$1" + openApiPath + "$3");
        } else {
            log.warn("Unable to replace the default URL of Swagger UI");
            return null;
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
        @ConfigItem(defaultValue = "false")
        boolean alwaysInclude;

        /**
         * If Swagger UI should be enabled. By default, Swagger UI is enabled.
         */
        @ConfigItem(defaultValue = "true")
        boolean enable;
    }

    private static final class CachedSwaggerUI implements Runnable {

        String cachedOpenAPIPath;
        String cachedDirectory;

        @Override
        public void run() {
            try {
                FileUtil.deleteDirectory(Paths.get(cachedDirectory));
            } catch (IOException e) {
                log.error("Failed to clean Swagger UI temp directory on shutdown", e);
            }
        }
    }
}
