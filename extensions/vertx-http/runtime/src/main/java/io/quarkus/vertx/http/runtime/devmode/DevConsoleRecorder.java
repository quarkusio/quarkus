package io.quarkus.vertx.http.runtime.devmode;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.testing.ContinuousTestingWebsocketListener;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class DevConsoleRecorder {

    private static final Logger LOG = Logger.getLogger(DevConsoleRecorder.class);

    public void addInfo(String groupId, String artifactId, String name, Supplier<? extends Object> supplier) {
        Map<String, Map<String, Object>> info = DevConsoleManager.getTemplateInfo();
        Map<String, Object> data = info.computeIfAbsent(groupId + "." + artifactId,
                new Function<String, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> apply(String s) {
                        return new HashMap<>();
                    }
                });
        data.put(name, supplier.get());
    }

    public Handler<RoutingContext> devConsoleHandler(String devConsoleFinalDestination,
            ShutdownContext shutdownContext) {
        shutdownContext.addShutdownTask(new CleanupDevConsoleTempDirectory(devConsoleFinalDestination));

        return new DevConsoleStaticHandler(devConsoleFinalDestination);
    }

    public Handler<RoutingContext> continousTestHandler(ShutdownContext context) {

        ContinuousTestWebSocketHandler handler = new ContinuousTestWebSocketHandler();
        ContinuousTestingWebsocketListener.setStateListener(handler);
        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                ContinuousTestingWebsocketListener.setStateListener(null);

            }
        });
        return handler;
    }

    private static final class CleanupDevConsoleTempDirectory implements Runnable {

        private final Path devConsoleFinalDestination;

        private CleanupDevConsoleTempDirectory(String devConsoleFinalDestination) {
            this.devConsoleFinalDestination = Paths.get(devConsoleFinalDestination);
        }

        @Override
        public void run() {
            try {
                Files.walkFileTree(devConsoleFinalDestination,
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
                LOG.error("Error cleaning up DEV Console temporary directory: " + devConsoleFinalDestination, e);
            }
        }
    }
}
