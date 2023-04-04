package io.quarkus.devui.runtime;

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

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethodName;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.FileSystemStaticHandler;
import io.quarkus.vertx.http.runtime.webjar.WebJarStaticHandler;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class DevUIRecorder {
    private static final Logger LOG = Logger.getLogger(DevUIRecorder.class);

    public void shutdownTask(ShutdownContext shutdownContext, String devUIBasePath) {
        shutdownContext.addShutdownTask(new DeleteDirectoryRunnable(devUIBasePath));
    }

    public void createJsonRpcRouter(BeanContainer beanContainer,
            Map<String, Map<JsonRpcMethodName, JsonRpcMethod>> extensionMethodsMap) {
        JsonRpcRouter jsonRpcRouter = beanContainer.beanInstance(JsonRpcRouter.class);
        jsonRpcRouter.populateJsonRPCMethods(extensionMethodsMap);
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

    public Handler<RoutingContext> vaadinRouterHandler(String basePath) {
        return new VaadinRouterHandler(basePath);
    }

    public Handler<RoutingContext> mvnpmHandler(Set<URL> mvnpmJarFiles) {
        return new MvnpmHandler(mvnpmJarFiles);
    }

    public Handler<RoutingContext> redirect() {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext rc) {
                // 308 because we also want to redirect other HTTP Methods (and not only GET).
                rc.response().putHeader("Location", "/q/dev-ui").setStatusCode(308).end();
            }
        };
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
