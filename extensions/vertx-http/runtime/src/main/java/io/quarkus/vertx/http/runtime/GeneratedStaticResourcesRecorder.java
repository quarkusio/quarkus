package io.quarkus.vertx.http.runtime;

import java.util.Set;
import java.util.function.Consumer;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.handlers.DevClasspathStaticHandler;
import io.quarkus.vertx.http.runtime.handlers.DevClasspathStaticHandlerOptions;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class GeneratedStaticResourcesRecorder {

    public static final String META_INF_RESOURCES = "META-INF/resources";
    private final RuntimeValue<HttpConfiguration> httpConfiguration;
    private final HttpBuildTimeConfig httpBuildTimeConfig;
    private Set<String> compressMediaTypes = Set.of();

    public GeneratedStaticResourcesRecorder(RuntimeValue<HttpConfiguration> httpConfiguration,
            HttpBuildTimeConfig httpBuildTimeConfig) {
        this.httpConfiguration = httpConfiguration;
        this.httpBuildTimeConfig = httpBuildTimeConfig;
    }

    public Handler<RoutingContext> createHandler(Set<String> generatedResources) {

        if (httpBuildTimeConfig.enableCompression && httpBuildTimeConfig.compressMediaTypes.isPresent()) {
            this.compressMediaTypes = Set.copyOf(httpBuildTimeConfig.compressMediaTypes.get());
        }
        StaticResourcesConfig config = httpConfiguration.getValue().staticResources;

        DevClasspathStaticHandlerOptions options = new DevClasspathStaticHandlerOptions.Builder()
                .indexPage(config.indexPage)
                .enableCompression(httpBuildTimeConfig.enableCompression)
                .compressMediaTypes(compressMediaTypes)
                .defaultEncoding(config.contentEncoding).build();
        return new DevClasspathStaticHandler(generatedResources,
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
