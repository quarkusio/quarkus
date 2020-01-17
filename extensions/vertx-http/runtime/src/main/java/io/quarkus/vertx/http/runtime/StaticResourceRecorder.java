package io.quarkus.vertx.http.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

@Recorder
public class StaticResourceRecorder {
    private static final Logger LOGGER = Logger.getLogger(StaticResourceRecorder.class.getName());

    public Handler<RoutingContext> handler(HttpBuildTimeConfig httpBuildTimeConfig, String name, String endpoint) {

        StaticResourceConfig.StaticResourceGeneralConfig config = httpBuildTimeConfig.staticConfig.config.get(name);

        final Path staticResourceDir = Paths.get(config.path);
        if (Files.notExists(staticResourceDir)) {
            LOGGER.warnf("Static file directory %s does not exist", staticResourceDir);
        }

        final StaticHandler staticHandler = StaticHandler.create()
                .setAllowRootFileSystemAccess(true)
                .setWebRoot(config.path)
                .setDirectoryListing(config.allowDirectoryListing)
                .setCachingEnabled(config.enableCache)
                .setMaxCacheSize(config.cacheSize.getAsInt())
                .setIndexPage(config.index.get())
                .setDefaultContentEncoding("UTF-8");

        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext routingContext) {
                if (routingContext.normalisedPath().equals(endpoint)) {
                    routingContext.response().setStatusCode(302);
                    routingContext.response().headers().set(HttpHeaders.LOCATION, endpoint + "/");
                    routingContext.response().end();
                    return;
                }

                String staticResource = routingContext.normalisedPath().replaceFirst(endpoint + "/", "");
                final Path staticResourcePath = staticResourceDir.resolve(staticResource);
                if (Files.exists(staticResourcePath)) {
                    staticHandler.handle(routingContext);
                } else {
                    routingContext.next();
                }
            }
        };
    }
}
