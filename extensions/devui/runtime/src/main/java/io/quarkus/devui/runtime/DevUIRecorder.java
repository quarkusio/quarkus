package io.quarkus.devui.runtime;

import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_DECODER;
import static io.quarkus.vertx.runtime.jackson.JsonUtil.BASE64_ENCODER;

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devjsonrpc.runtime.DevJsonRpcRecorder;
import io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonTypeAdapter;
import io.quarkus.devui.runtime.js.DevUIWebSocketHandler;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class DevUIRecorder {
    private static final Logger LOG = Logger.getLogger(DevUIRecorder.class);

    @SuppressWarnings("unchecked")
    public void initializeJsonRpcCodec(BeanContainer beanContainer) {
        JsonMapper.Factory factory = JsonMapper.Factory.deploymentLinker().createLink(
                DevConsoleManager.getGlobal(DevJsonRpcRecorder.DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY));
        JsonMapper jsonMapper = factory.create(
                new JsonTypeAdapter<>(JsonObject.class, JsonObject::getMap, JsonObject::new),
                new JsonTypeAdapter<>(JsonArray.class, JsonArray::getList, JsonArray::new),
                new JsonTypeAdapter<>(Buffer.class, buffer -> BASE64_ENCODER.encodeToString(buffer.getBytes()), text -> {
                    try {
                        return Buffer.buffer(BASE64_DECODER.decode(text));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Expected a base64 encoded byte array, got: " + text, e);
                    }
                }));
        JsonRpcRouter jsonRpcRouter = beanContainer.beanInstance(JsonRpcRouter.class);
        jsonRpcRouter.initializeCodec(jsonMapper);
    }

    public void shutdownTask(ShutdownContext shutdownContext, String devUIBasePath) {
        shutdownContext.addShutdownTask(new DeleteDirectoryRunnable(devUIBasePath));
    }

    public Handler<RoutingContext> devUIWebSocketHandler() {
        return new DevUIWebSocketHandler();
    }

    public Handler<RoutingContext> uiHandler(String finalDestination,
            String path,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations,
            ShutdownContext shutdownContext) {

        WebJarStaticHandler handler = new WebJarStaticHandler(finalDestination, path, webRootConfigurations);
        shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));
        return handler;
    }

    public Handler<RoutingContext> buildTimeStaticHandler(BeanContainer beanContainer,
            String basePath,
            Map<String, String> urlAndPath,
            Map<String, String> descriptions,
            Map<String, String> mcpDefaultEnabled,
            Map<String, String> contentTypes) {
        DevUIBuildTimeStaticService buildTimeStaticService = beanContainer.beanInstance(DevUIBuildTimeStaticService.class);
        buildTimeStaticService.addData(basePath, urlAndPath, descriptions, mcpDefaultEnabled, contentTypes);

        io.quarkus.devmcp.runtime.McpBuildTimeData mcpBuildTimeData = beanContainer
                .beanInstance(io.quarkus.devmcp.runtime.McpBuildTimeData.class);
        mcpBuildTimeData.addData(urlAndPath, descriptions, mcpDefaultEnabled, contentTypes);

        return new DevUIBuildTimeStaticHandler();
    }

    public Handler<RoutingContext> endpointInfoHandler(String basePath) {
        return new EndpointInfoHandler(basePath);
    }

    public Handler<RoutingContext> vaadinRouterHandler(String basePath) {
        return new VaadinRouterHandler(basePath);
    }

    public Handler<RoutingContext> mvnpmHandler(String root, Set<URL> mvnpmJarFiles) {
        return new MvnpmHandler(root, mvnpmJarFiles);
    }

    public Handler<RoutingContext> redirect(String contextRoot) {
        return redirect(contextRoot, null);
    }

    public Handler<RoutingContext> redirect(String contextRoot, String page) {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext rc) {
                // Initially we were using 308 (MOVED PERMANENTLY) because we also want to redirect other HTTP Methods
                // (and not only GET).
                // However, it caused issues with browser caches and prevented users to have applications using Quarkus 2
                // and Quarkus 3 at the same time. So, we decided to switch to FOUND (302)
                // See https://github.com/quarkusio/quarkus/issues/33658 for more context.
                String location = contextRoot + "dev-ui";
                if (page != null) {
                    location = location + "/" + page;
                }
                rc.response()
                        .putHeader("Location", location)
                        .setStatusCode(HttpResponseStatus.FOUND.code()).end();
            }
        };
    }

    public Handler<RoutingContext> createLocalHostOnlyFilter(List<String> hosts) {
        return new LocalHostOnlyFilter(hosts);
    }

    public Handler<RoutingContext> createDevUICorsFilter(List<String> hosts) {
        return new DevUICORSFilter(hosts);
    }

    private static final class DeleteDirectoryRunnable implements Runnable {

        private final Path directory;

        private DeleteDirectoryRunnable(String directory) {
            this.directory = Paths.get(directory);
        }

        @Override
        public void run() {
            try {
                Files.walkFileTree(directory,
                        new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }
                        });
            } catch (IOException e) {
                LOG.error("Error cleaning up dev-ui temporary directory: " + directory, e);
            }
        }
    }
}
