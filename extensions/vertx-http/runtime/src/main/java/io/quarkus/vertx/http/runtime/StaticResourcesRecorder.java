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
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;

@Recorder
public class StaticResourcesRecorder {

    public static final String META_INF_RESOURCES = "META-INF/resources";

    private static volatile List<Path> hotDeploymentResourcePaths;

    final RuntimeValue<HttpConfiguration> httpConfiguration;
    final HttpBuildTimeConfig httpBuildTimeConfig;
    private Set<String> compressMediaTypes = Set.of();

    public StaticResourcesRecorder(RuntimeValue<HttpConfiguration> httpConfiguration,
            HttpBuildTimeConfig httpBuildTimeConfig) {
        this.httpConfiguration = httpConfiguration;
        this.httpBuildTimeConfig = httpBuildTimeConfig;
    }

    public static void setHotDeploymentResources(List<Path> resources) {
        hotDeploymentResourcePaths = resources;
    }

    public Consumer<Route> start(Set<String> knownPaths) {
        if (httpBuildTimeConfig.enableCompression && httpBuildTimeConfig.compressMediaTypes.isPresent()) {
            this.compressMediaTypes = Set.copyOf(httpBuildTimeConfig.compressMediaTypes.get());
        }
        List<Handler<RoutingContext>> handlers = new ArrayList<>();
        StaticResourcesConfig config = httpConfiguration.getValue().staticResources;

        if (hotDeploymentResourcePaths != null && !hotDeploymentResourcePaths.isEmpty()) {
            for (Path resourcePath : hotDeploymentResourcePaths) {
                String root = resourcePath.toAbsolutePath().toString();
                StaticHandler staticHandler = StaticHandler.create(FileSystemAccess.ROOT, root)
                        .setDefaultContentEncoding(config.contentEncoding.name())
                        .setCachingEnabled(false)
                        .setIndexPage(config.indexPage)
                        .setIncludeHidden(config.includeHidden)
                        .setEnableRangeSupport(config.enableRangeSupport);
                handlers.add(new Handler<>() {
                    @Override
                    public void handle(RoutingContext ctx) {
                        try {
                            compressIfNeeded(ctx, ctx.normalizedPath());
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
            StaticHandler staticHandler = StaticHandler.create(META_INF_RESOURCES)
                    .setDefaultContentEncoding("UTF-8")
                    .setCachingEnabled(config.cachingEnabled)
                    .setIndexPage(config.indexPage)
                    .setIncludeHidden(config.includeHidden)
                    .setEnableRangeSupport(config.enableRangeSupport)
                    .setMaxCacheSize(config.maxCacheSize)
                    .setCacheEntryTimeout(config.cacheEntryTimeout.toMillis())
                    .setMaxAgeSeconds(config.maxAge.toSeconds());
            // normalize index page like StaticHandler because its not expose
            // TODO: create a converter to normalize filename in config.indexPage?
            final String indexPage = (config.indexPage.charAt(0) == '/')
                    ? config.indexPage.substring(1)
                    : config.indexPage;
            handlers.add(new Handler<>() {
                @Override
                public void handle(RoutingContext ctx) {
                    String rel = ctx.mountPoint() == null ? ctx.normalizedPath()
                            : ctx.normalizedPath().substring(
                                    // let's be extra careful here in case Vert.x normalizes the mount points at some point
                                    ctx.mountPoint().endsWith("/") ? ctx.mountPoint().length() - 1 : ctx.mountPoint().length());
                    // check effective path, otherwise the index page when path ends with '/'
                    if (knownPaths.contains(rel) || (rel.endsWith("/") && knownPaths.contains(rel.concat(indexPage)))) {
                        compressIfNeeded(ctx, rel);
                        staticHandler.handle(ctx);
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

    private void compressIfNeeded(RoutingContext ctx, String path) {
        if (httpBuildTimeConfig.enableCompression && isCompressed(path)) {
            // VertxHttpRecorder is adding "Content-Encoding: identity" to all requests if compression is enabled.
            // Handlers can remove the "Content-Encoding: identity" header to enable compression.
            ctx.response().headers().remove(HttpHeaders.CONTENT_ENCODING);
        }
    }

    private boolean isCompressed(String path) {
        if (compressMediaTypes.isEmpty()) {
            return false;
        }
        final String resourcePath = path.endsWith("/") ? path + StaticHandler.DEFAULT_INDEX_PAGE : path;
        final String contentType = MimeMapping.getMimeTypeForFilename(resourcePath);
        return contentType != null && compressMediaTypes.contains(contentType);
    }

}
