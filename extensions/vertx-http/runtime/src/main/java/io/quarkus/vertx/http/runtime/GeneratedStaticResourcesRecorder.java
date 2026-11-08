package io.quarkus.vertx.http.runtime;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.quarkus.vertx.http.runtime.handlers.DevClasspathStaticHandlerOptions;
import io.quarkus.vertx.http.runtime.handlers.DevStaticHandler;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

public class GeneratedStaticResourcesRecorder {

    public static final String META_INF_RESOURCES = "META-INF/resources";

    private GeneratedStaticResourcesRecorder() {
    }

    public static Handler<RoutingContext> createHandler(VertxHttpConfig httpConfig,
            VertxHttpBuildTimeConfig httpBuildTimeConfig, Set<String> generatedClasspathResources,
            Map<String, String> generatedFilesResources) {

        StaticResourcesConfig config = httpConfig.staticResources();

        DevClasspathStaticHandlerOptions options = new DevClasspathStaticHandlerOptions.Builder()
                .indexPage(config.indexPage())
                .httpBuildTimeConfig(httpBuildTimeConfig)
                .defaultEncoding(config.contentEncoding())
                .build();
        return new DevStaticHandler(generatedClasspathResources,
                generatedFilesResources,
                options);

    }

    public static Consumer<Route> createRouteCustomizer() {
        return route -> {
            route.method(HttpMethod.GET);
            route.method(HttpMethod.HEAD);
            route.method(HttpMethod.OPTIONS);
        };
    }
}
