package io.quarkus.vertx.http.runtime;

import static io.quarkus.vertx.http.runtime.RoutingUtils.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.handlers.ClasspathStaticResourcesHandler;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;

@Recorder
public class StaticResourcesRecorder {

    public static final String META_INF_RESOURCES = "META-INF/resources";

    private static volatile List<Path> hotDeploymentResourcePaths;

    private final VertxHttpBuildTimeConfig httpBuildTimeConfig;
    private final RuntimeValue<VertxHttpConfig> httpConfig;

    public StaticResourcesRecorder(
            final VertxHttpBuildTimeConfig httpBuildTimeConfig,
            final RuntimeValue<VertxHttpConfig> httpConfig) {
        this.httpBuildTimeConfig = httpBuildTimeConfig;
        this.httpConfig = httpConfig;
    }

    public static void setHotDeploymentResources(List<Path> resources) {
        hotDeploymentResourcePaths = resources;
    }

    public Consumer<Route> start(Set<String> knownPaths) {
        List<Handler<RoutingContext>> handlers = new ArrayList<>();
        Set<String> compressMediaTypes;
        if (httpBuildTimeConfig.enableCompression() && httpBuildTimeConfig.compressMediaTypes().isPresent()) {
            compressMediaTypes = Set.copyOf(httpBuildTimeConfig.compressMediaTypes().get());
        } else {
            compressMediaTypes = Set.of();
        }
        StaticResourcesConfig config = httpConfig.getValue().staticResources();

        if (hotDeploymentResourcePaths != null && !hotDeploymentResourcePaths.isEmpty()) {
            for (Path resourcePath : hotDeploymentResourcePaths) {
                String root = resourcePath.toAbsolutePath().toString();
                StaticHandler staticHandler = StaticHandler.create(FileSystemAccess.ROOT, root)
                        .setDefaultContentEncoding(config.contentEncoding().name())
                        .setCachingEnabled(false)
                        .setIndexPage(config.indexPage())
                        .setIncludeHidden(config.includeHidden())
                        .setEnableRangeSupport(config.enableRangeSupport())
                        .setDirectoryListing(config.directoryListing())
                        .setSendVaryHeader(config.sendVaryHeader());
                handlers.add(new Handler<>() {
                    @Override
                    public void handle(RoutingContext ctx) {
                        try {
                            String path = getNormalizedAndDecodedPath(ctx);
                            if (path == null) {
                                ctx.fail(HttpResponseStatus.BAD_REQUEST.code());
                                return;
                            }
                            compressIfNeeded(httpBuildTimeConfig, compressMediaTypes, ctx, path);
                            staticHandler.handle(ctx);
                        } catch (Exception e) {
                            // on Windows, the drive in file path screws up cache lookup
                            // so just punt to next handler
                            ctx.next();
                        }
                    }
                });
            }
        }
        if (!knownPaths.isEmpty()) {
            ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
            // normalize index page like StaticHandler because its not expose
            // TODO: create a converter to normalize filename in config.indexPage?
            final String indexPage = (config.indexPage().charAt(0) == '/')
                    ? config.indexPage().substring(1)
                    : config.indexPage();
            Handler<RoutingContext> handler = new ClasspathStaticResourcesHandler(knownPaths, indexPage, config,
                    httpBuildTimeConfig,
                    compressMediaTypes, currentCl);
            handlers.add(new Handler<>() {
                @Override
                public void handle(RoutingContext ctx) {
                    String rel = resolvePath(ctx);
                    if (rel == null) {
                        ctx.fail(HttpResponseStatus.BAD_REQUEST.code());
                        return;
                    }
                    // Delegate to dedicated handler (no Vert.x StaticHandler) while preserving the old known-path fast path.
                    // Ensure we don't lose the correct TCCL to Vert.x when we fall through.
                    try {
                        handler.handle(ctx);
                    } finally {
                        Thread.currentThread().setContextClassLoader(currentCl);
                    }
                }
            });
        }

        return new Consumer<>() {

            @Override
            public void accept(Route route) {
                // Restrict the route for static resources to HEAD and GET
                // No other HTTP methods should be used
                route.method(HttpMethod.GET);
                route.method(HttpMethod.HEAD);
                route.method(HttpMethod.OPTIONS);
                for (Handler<RoutingContext> i : handlers) {
                    route.handler(i);
                }
            }
        };
    }

}
