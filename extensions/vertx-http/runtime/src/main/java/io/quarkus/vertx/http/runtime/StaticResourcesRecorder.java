package io.quarkus.vertx.http.runtime;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

@Recorder
public class StaticResourcesRecorder {

    public static final String META_INF_RESOURCES = "META-INF/resources";

    private static volatile List<Path> hotDeploymentResourcePaths;

    final RuntimeValue<HttpConfiguration> httpConfiguration;
    final HttpBuildTimeConfig httpBuildTimeConfig;

    public StaticResourcesRecorder(RuntimeValue<HttpConfiguration> httpConfiguration,
            HttpBuildTimeConfig httpBuildTimeConfig) {
        this.httpConfiguration = httpConfiguration;
        this.httpBuildTimeConfig = httpBuildTimeConfig;
    }

    public static void setHotDeploymentResources(List<Path> resources) {
        hotDeploymentResourcePaths = resources;
    }

    public Consumer<Route> start(Set<String> knownPaths) {

        List<Handler<RoutingContext>> handlers = new ArrayList<>();

        if (hotDeploymentResourcePaths != null && !hotDeploymentResourcePaths.isEmpty()) {
            for (Path resourcePath : hotDeploymentResourcePaths) {
                String root = resourcePath.toAbsolutePath().toString();
                StaticHandler staticHandler = StaticHandler.create();
                staticHandler.setCachingEnabled(false);
                staticHandler.setAllowRootFileSystemAccess(true);
                staticHandler.setWebRoot(root);
                staticHandler.setDefaultContentEncoding("UTF-8");
                handlers.add(new Handler<>() {
                    @Override
                    public void handle(RoutingContext event) {
                        try {
                            staticHandler.handle(event);
                        } catch (Exception e) {
                            // on Windows, the drive in file path screws up cache lookup
                            // so just punt to next handler
                            event.next();
                        }
                    }
                });
            }
        }
        if (!knownPaths.isEmpty()) {
            ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
            StaticHandler staticHandler = StaticHandler.create(META_INF_RESOURCES).setDefaultContentEncoding("UTF-8");
            handlers.add(new Handler<>() {
                @Override
                public void handle(RoutingContext ctx) {
                    String rel = ctx.mountPoint() == null ? ctx.normalizedPath()
                            : ctx.normalizedPath().substring(
                                    // let's be extra careful here in case Vert.x normalizes the mount points at some point
                                    ctx.mountPoint().endsWith("/") ? ctx.mountPoint().length() - 1 : ctx.mountPoint().length());
                    if (knownPaths.contains(rel)) {
                        staticHandler.handle(ctx);
                        if (httpBuildTimeConfig.enableCompression && isCompressed(rel)) {
                            // Remove the "Content-Encoding: identity" header and enable compression
                            ctx.response().headers().remove(HttpHeaders.CONTENT_ENCODING);
                        }
                    } else {
                        // make sure we don't lose the correct TCCL to Vert.x...
                        Thread.currentThread().setContextClassLoader(currentCl);
                        ctx.next();
                    }
                }
            });
        }

        return new Consumer<>() {

            @Override
            public void accept(Route route) {
                for (Handler<RoutingContext> i : handlers) {
                    route.handler(i);
                }
            }
        };
    }

    private boolean isCompressed(String path) {
        String suffix;
        int lastDot = path.lastIndexOf('.');
        if (lastDot != -1 && lastDot != path.length() - 1) {
            suffix = path.substring(lastDot + 1);
        } else {
            suffix = null;
        }
        String contentType = MimeMapping.getMimeTypeForExtension(suffix);
        return httpBuildTimeConfig.compressMediaTypes.orElse(List.of()).contains(contentType);
    }

}
