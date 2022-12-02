package io.quarkus.vertx.http.runtime.devmode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.testing.ContinuousTestingSharedStateManager;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class DevConsoleRecorder {

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

    public void initConfigFun() {
        SmallRyeConfig config = (SmallRyeConfig) ConfigProvider.getConfig();
        DevConsoleManager.setGlobal("devui-config-fun", new Function<String, Optional<String>>() {
            @Override
            public Optional<String> apply(String name) {
                return config.getOptionalValue(name, String.class);
            }
        });
    }

    /**
     *
     * @param devConsoleFinalDestination
     * @param shutdownContext
     * @return
     * @deprecated use {@link #fileSystemStaticHandler(List, ShutdownContext)}
     */
    @Deprecated
    public Handler<RoutingContext> devConsoleHandler(String devConsoleFinalDestination,
            ShutdownContext shutdownContext) {
        List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations = new ArrayList<>();
        webRootConfigurations.add(
                new FileSystemStaticHandler.StaticWebRootConfiguration(devConsoleFinalDestination, ""));

        return fileSystemStaticHandler(webRootConfigurations, shutdownContext);
    }

    public Handler<RoutingContext> fileSystemStaticHandler(
            List<FileSystemStaticHandler.StaticWebRootConfiguration> webRootConfigurations,
            ShutdownContext shutdownContext) {

        FileSystemStaticHandler fileSystemStaticHandler = new FileSystemStaticHandler(webRootConfigurations);

        shutdownContext.addShutdownTask(new ShutdownContext.CloseRunnable(fileSystemStaticHandler));

        return fileSystemStaticHandler;
    }

    public Handler<RoutingContext> continuousTestHandler(ShutdownContext context) {

        ContinuousTestWebSocketHandler handler = new ContinuousTestWebSocketHandler();
        ContinuousTestingSharedStateManager.addStateListener(handler);
        context.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                ContinuousTestingSharedStateManager.removeStateListener(handler);

            }
        });
        return handler;
    }
}
