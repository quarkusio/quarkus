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
import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethodName;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devui.runtime.jsonrpc.json.JsonTypeAdapter;
import io.quarkus.runtime.RuntimeValue;
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
    public static final String DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY = "dev-ui-databind-codec-builder";

    public void shutdownTask(ShutdownContext shutdownContext, String devUIBasePath) {
        shutdownContext.addShutdownTask(new DeleteDirectoryRunnable(devUIBasePath));
    }

    public void createJsonRpcRouter(BeanContainer beanContainer,
            Map<String, Map<JsonRpcMethodName, JsonRpcMethod>> extensionMethodsMap,
            List<String> deploymentMethods,
            List<String> deploymentSubscriptions,
            Map<String, RuntimeValue> recordedValues) {
        JsonRpcRouter jsonRpcRouter = beanContainer.beanInstance(JsonRpcRouter.class);
        jsonRpcRouter.populateJsonRPCRuntimeMethods(extensionMethodsMap);
        jsonRpcRouter.setJsonRPCDeploymentActions(deploymentMethods, deploymentSubscriptions);
        if (recordedValues != null && !recordedValues.isEmpty()) {
            jsonRpcRouter.setRecordedValues(recordedValues);
        }

        jsonRpcRouter.initializeCodec(createJsonMapper());
    }

    private JsonMapper createJsonMapper() {
        // We use a codec defined in the deployment module
        // because that module always has access to Jackson-Databind regardless of the application dependencies.
        JsonMapper.Factory factory = JsonMapper.Factory.deploymentLinker().createLink(
                DevConsoleManager.getGlobal(DEV_MANAGER_GLOBALS_JSON_MAPPER_FACTORY));
        // We need to pass some information so that the mapper, who lives in the deployment classloader,
        // knows how to deal with JsonObject/JsonArray/JsonBuffer, who live in the runtime classloader.
        return factory.create(new JsonTypeAdapter<>(JsonObject.class, JsonObject::getMap, JsonObject::new),
                new JsonTypeAdapter<JsonArray, List<?>>(JsonArray.class, JsonArray::getList, JsonArray::new),
                new JsonTypeAdapter<>(Buffer.class, buffer -> BASE64_ENCODER.encodeToString(buffer.getBytes()), text -> {
                    try {
                        return Buffer.buffer(BASE64_DECODER.decode(text));
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Expected a base64 encoded byte array, got: " + text, e);
                    }
                }));
    }

    public Handler<RoutingContext> communicationHandler() {
        return new DevUIWebSocket();
    }

    public Handler<RoutingContext> uiHandler(String finalDestination,
            String path,
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations,
            ShutdownContext shutdownContext) {

        WebJarStaticHandler handler = new WebJarStaticHandler(finalDestination, path, webRootConfigurations);
        shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(handler));
        return handler;
    }

    public Handler<RoutingContext> buildTimeStaticHandler(String basePath, Map<String, String> urlAndPath) {
        return new DevUIBuildTimeStaticHandler(basePath, urlAndPath);
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
