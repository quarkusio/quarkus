package io.quarkus.logging.manager.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.builder.Version;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.deployment.util.WebJarUtil;
import io.quarkus.logging.manager.runtime.LoggerManagerRecorder;
import io.quarkus.logging.manager.runtime.LoggingManagerRuntimeConfig;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.logstream.HistoryHandler;
import io.quarkus.vertx.http.runtime.logstream.JsonFormatter;
import io.quarkus.vertx.http.runtime.logstream.LogStreamWebSocket;
import io.quarkus.vertx.http.runtime.logstream.WebSocketHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

class LoggingManagerProcessor {
    private static final String FEATURE = "logging-manager";

    // For the UI
    private static final String UI_WEBJAR_GROUP_ID = "io.quarkus";
    private static final String UI_WEBJAR_ARTIFACT_ID = "quarkus-logging-manager";

    private static final String UI_FINAL_DESTINATION = "META-INF/logging-manager-files";

    private static final String STATIC_RESOURCE_FOLDER = "dev-static/";
    private static final String INDEX_HTML = "index.html";

    private final Config config = ConfigProvider.getConfig();

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.LOGGING_MANAGER);
    }

    @BuildStep
    void includeUiAndWebsocket(
            BuildProducer<AdditionalBeanBuildItem> annotatedProducer,
            BuildProducer<RouteBuildItem> routeProducer,
            BuildProducer<LoggingManagerBuildItem> loggingManagerBuildProducer,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> notFoundPageDisplayableEndpointProducer,
            HttpRootPathBuildItem httpRootPathBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingManagerConfig loggingManagerConfig) throws Exception {

        if ("/".equals(loggingManagerConfig.ui.rootPath)) {
            throw new ConfigurationError(
                    "quarkus.logging-manager.ui.root-path was set to \"/\", this is not allowed as it blocks the application from serving anything else.");
        }

        AppArtifact artifact = WebJarUtil.getAppArtifact(curateOutcomeBuildItem, UI_WEBJAR_GROUP_ID,
                UI_WEBJAR_ARTIFACT_ID);
        AppArtifact userApplication = curateOutcomeBuildItem.getEffectiveModel().getAppArtifact();

        String uiPath = httpRootPathBuildItem
                .adjustPath(nonApplicationRootPathBuildItem.adjustPath(loggingManagerConfig.ui.rootPath));

        if (launchMode.getLaunchMode().isDevOrTest()) {
            // The static resources

            Path tempPath = WebJarUtil.createResourcesDirectory(userApplication, artifact);

            Path indexHtml = Paths.get(tempPath.toString(), INDEX_HTML);
            if (!Files.exists(indexHtml)) {
                Files.createFile(indexHtml);
            }
            String indexHtmlContent = getIndexHtmlContents(nonApplicationRootPathBuildItem.getFrameworkRootPath(),
                    "/dev/logstream");

            IoUtils.writeFile(indexHtml, indexHtmlContent);

            loggingManagerBuildProducer
                    .produce(new LoggingManagerBuildItem(tempPath.toAbsolutePath().toString(), uiPath));

            notFoundPageDisplayableEndpointProducer.produce(new NotFoundPageDisplayableEndpointBuildItem(
                    nonApplicationRootPathBuildItem.adjustPath(loggingManagerConfig.ui.rootPath + "/"),
                    "Quarkus Logging manager"));

        } else if (loggingManagerConfig.ui.alwaysInclude) {
            // Make sure the WebSocket gets included.
            annotatedProducer.produce(AdditionalBeanBuildItem.unremovableOf(LogStreamWebSocket.class));

            // Get the index.html
            String indexHtmlContent = getIndexHtmlContents(nonApplicationRootPathBuildItem.getFrameworkRootPath(),
                    loggingManagerConfig.basePath + "/logstream");
            // Update the resource Url to be relative
            indexHtmlContent = indexHtmlContent.replaceAll(nonApplicationRootPathBuildItem.adjustPath("/dev/resources/"), "");

            String fileName = UI_FINAL_DESTINATION + "/" + INDEX_HTML;
            generatedResourceProducer.produce(new GeneratedResourceBuildItem(fileName, indexHtmlContent.getBytes()));
            nativeImageResourceProducer.produce(new NativeImageResourceBuildItem(fileName));

            addStaticResource(generatedResourceProducer, nativeImageResourceProducer);

            loggingManagerBuildProducer.produce(new LoggingManagerBuildItem(UI_FINAL_DESTINATION, uiPath));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void registerLoggingManagerHandler(
            BuildProducer<RouteBuildItem> routeProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer,
            LoggerManagerRecorder recorder,
            LoggingManagerRuntimeConfig runtimeConfig,
            LoggingManagerBuildItem loggingManagerBuildItem,
            LaunchModeBuildItem launchMode,
            LoggingManagerConfig loggingManagerConfig) throws Exception {

        if (shouldInclude(launchMode, loggingManagerConfig)) {
            Handler<RoutingContext> handler = recorder.uiHandler(loggingManagerBuildItem.getLoggingManagerFinalDestination(),
                    loggingManagerBuildItem.getLoggingManagerPath(), runtimeConfig);

            routeProducer.produce(new RouteBuildItem.Builder()
                    .route(loggingManagerConfig.ui.rootPath)
                    .handler(handler)
                    .nonApplicationRoute(false)
                    .build());
            routeProducer.produce(new RouteBuildItem.Builder()
                    .route(loggingManagerConfig.ui.rootPath + "/*")
                    .handler(handler)
                    .nonApplicationRoute(false)
                    .build());

            // Add the log stream (In dev mode, the stream is already available at /dev/logstream)
            if (!launchMode.getLaunchMode().isDevOrTest() && loggingManagerConfig.ui.alwaysInclude) {

                reflectiveClassProducer.produce(new ReflectiveClassBuildItem(true, true,
                        LogStreamWebSocket.class,
                        HistoryHandler.class,
                        WebSocketHandler.class,
                        JsonFormatter.class));
                Handler<RoutingContext> logStreamWebSocketHandler = recorder.logStreamWebSocketHandler(runtimeConfig);

                routeProducer.produce(new RouteBuildItem.Builder()
                        .route(loggingManagerConfig.basePath + "/logstream")
                        .handler(logStreamWebSocketHandler)
                        .nonApplicationRoute(false)
                        .build());
            }
        }
    }

    private String getIndexHtmlContents(String nonApplicationRootPath, String streamingPath) throws IOException {
        // Get the loggermanager html resources from Dev UI
        try (InputStream nav = LoggingManagerProcessor.class.getClassLoader()
                .getResourceAsStream("dev-templates/logmanagerNav.html");
                InputStream log = LoggingManagerProcessor.class.getClassLoader()
                        .getResourceAsStream("dev-templates/logmanagerLog.html");
                InputStream modals = LoggingManagerProcessor.class.getClassLoader()
                        .getResourceAsStream("dev-templates/logmanagerModals.html")) {

            String navContent = new String(IoUtil.readBytes(nav));
            String logContent = new String(IoUtil.readBytes(log));
            String modalsContent = new String(IoUtil.readBytes(modals));

            try (InputStream index = LoggingManagerProcessor.class.getClassLoader()
                    .getResourceAsStream("META-INF/resources/template/loggermanager.html")) {

                String indexHtmlContent = new String(IoUtil.readBytes(index));

                // Add the terminal (might contain vars)
                indexHtmlContent = indexHtmlContent.replaceAll("\\{navContent}",
                        navContent);
                indexHtmlContent = indexHtmlContent.replaceAll("\\{logContent}",
                        logContent);
                indexHtmlContent = indexHtmlContent.replaceAll("\\{modalsContent}",
                        modalsContent);

                // Make sure the non apllication path and streaming path is replaced
                indexHtmlContent = indexHtmlContent.replaceAll("\\{frameworkRootPath}",
                        nonApplicationRootPath);
                indexHtmlContent = indexHtmlContent.replaceAll("\\{streamingPath}",
                        streamingPath);

                // Make sure the application name and version is replaced
                indexHtmlContent = indexHtmlContent.replaceAll("\\{applicationName}",
                        config.getOptionalValue("quarkus.application.name", String.class).orElse(""));
                indexHtmlContent = indexHtmlContent.replaceAll("\\{applicationVersion}",
                        config.getOptionalValue("quarkus.application.version", String.class).orElse(""));
                indexHtmlContent = indexHtmlContent.replaceAll("\\{quarkusVersion}",
                        Version.getVersion());

                return indexHtmlContent;
            }
        }
    }

    private void addStaticResource(BuildProducer<GeneratedResourceBuildItem> generatedResourceProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer) throws IOException, URISyntaxException {

        URI uri = LoggingManagerProcessor.class.getClassLoader().getResource(STATIC_RESOURCE_FOLDER).toURI();

        FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object> emptyMap());
        Path myPath = fileSystem.getPath(STATIC_RESOURCE_FOLDER);

        Stream<Path> walk = Files.walk(myPath, 5);
        for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
            Path staticResource = it.next();
            if (!Files.isDirectory(staticResource) && Files.isRegularFile(staticResource)) {
                String fileName = UI_FINAL_DESTINATION + "/"
                        + staticResource.toString().substring(STATIC_RESOURCE_FOLDER.length() + 1);
                byte[] content = Files.readAllBytes(staticResource);
                generatedResourceProducer.produce(new GeneratedResourceBuildItem(fileName, content));
                nativeImageResourceProducer.produce(new NativeImageResourceBuildItem(fileName));
            }
        }
    }

    private static boolean shouldInclude(LaunchModeBuildItem launchMode, LoggingManagerConfig loggingManagerConfig) {
        return launchMode.getLaunchMode().isDevOrTest() || loggingManagerConfig.ui.alwaysInclude;
    }
}
