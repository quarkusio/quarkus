package io.quarkus.vertx.http.runtime;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

@Recorder
public class StaticResourcesRecorder {

    public static final String META_INF_RESOURCES = "META-INF/resources";

    private static volatile Set<String> knownPaths;
    private static volatile List<Path> hotDeploymentResourcePaths;

    public static void setHotDeploymentResources(List<Path> resources) {
        hotDeploymentResourcePaths = resources;
    }

    public void staticInit(Set<String> knownPaths) {
        StaticResourcesRecorder.knownPaths = knownPaths;
    }

    public Consumer<Route> start() {

        List<Handler<RoutingContext>> handlers = new ArrayList<>();

        if (hotDeploymentResourcePaths != null && !hotDeploymentResourcePaths.isEmpty()) {
            for (Path resourcePath : hotDeploymentResourcePaths) {
                String root = resourcePath.toAbsolutePath().toString();
                StaticHandler staticHandler = StaticHandler.create();
                staticHandler.setCachingEnabled(false);
                staticHandler.setAllowRootFileSystemAccess(true);
                staticHandler.setWebRoot(root);
                staticHandler.setDefaultContentEncoding("UTF-8");
                handlers.add(event -> {
                    try {
                        staticHandler.handle(event);
                    } catch (Exception e) {
                        // on Windows, the drive in file path screws up cache lookup
                        // so just punt to next handler
                        event.next();
                    }
                });
            }
        }
        if (!knownPaths.isEmpty()) {
            StaticHandler staticHandler = StaticHandler.create(META_INF_RESOURCES).setDefaultContentEncoding("UTF-8");
            handlers.add(ctx -> {
                String rel = ctx.mountPoint() == null ? ctx.normalisedPath()
                        : ctx.normalisedPath().substring(ctx.mountPoint().length());
                if (knownPaths.contains(rel)) {
                    staticHandler.handle(ctx);
                } else {
                    ctx.next();
                }
            });
        }

        return new Consumer<Route>() {

            @Override
            public void accept(Route route) {
                for (Handler<RoutingContext> i : handlers) {
                    route.handler(i);
                }
            }
        };
    }

}
