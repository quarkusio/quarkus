package io.quarkus.vertx.http.runtime;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.handlers.DevClasspathStaticHandlerOptions;
import io.quarkus.vertx.http.runtime.handlers.DevStaticHandler;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class GeneratedStaticResourcesRecorder {

    public static final String META_INF_RESOURCES = "META-INF/resources";
    private final RuntimeValue<VertxHttpConfig> httpConfig;
    private final VertxHttpBuildTimeConfig httpBuildTimeConfig;

    public GeneratedStaticResourcesRecorder(RuntimeValue<VertxHttpConfig> httpConfig,
            VertxHttpBuildTimeConfig httpBuildTimeConfig) {
        this.httpConfig = httpConfig;
        this.httpBuildTimeConfig = httpBuildTimeConfig;
    }

    public Handler<RoutingContext> createHandler(Set<String> generatedClasspathResources,
            Map<String, String> generatedFilesResources) {

        StaticResourcesConfig config = httpConfig.getValue().staticResources();

        DevClasspathStaticHandlerOptions options = new DevClasspathStaticHandlerOptions.Builder()
                .indexPage(config.indexPage())
                .httpBuildTimeConfig(httpBuildTimeConfig)
                .defaultEncoding(config.contentEncoding())
                .build();
        return new DevStaticHandler(generatedClasspathResources,
                generatedFilesResources,
                options);

    }

    public Consumer<Route> createRouteCustomizer() {
        return route -> {
            route.method(HttpMethod.GET);
            route.method(HttpMethod.HEAD);
            route.method(HttpMethod.OPTIONS);
        };
    }
}
