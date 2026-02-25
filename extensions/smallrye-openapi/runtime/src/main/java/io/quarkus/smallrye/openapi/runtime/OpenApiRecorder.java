package io.quarkus.smallrye.openapi.runtime;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import io.quarkus.smallrye.openapi.runtime.filter.AutoSecurityFilter;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.filters.Filter;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class OpenApiRecorder {
    private final RuntimeValue<OpenApiRuntimeConfig> openApiConfig;
    private final RuntimeValue<VertxHttpConfig> httpConfig;

    public OpenApiRecorder(
            final RuntimeValue<OpenApiRuntimeConfig> openApiConfig,
            final RuntimeValue<VertxHttpConfig> httpConfig) {
        this.openApiConfig = openApiConfig;
        this.httpConfig = httpConfig;
    }

    public Consumer<Route> corsFilter(Filter filter) {
        if (httpConfig.getValue().cors().enabled() && filter.getHandler() != null) {
            return new Consumer<Route>() {
                @Override
                public void accept(Route route) {
                    route.order(-1 * filter.getPriority()).handler(filter.getHandler());
                }
            };
        }
        return null;
    }

    public Handler<RoutingContext> handler(String documentName, boolean alwaysRunFilter) {
        if (openApiConfig.getValue().enable().orElse(openApiConfig.getValue().enabled())) {
            return new OpenApiHandler(documentName, alwaysRunFilter);
        } else {
            return new OpenApiNotFoundHandler();
        }
    }

    public void setupClDevMode(ShutdownContext shutdownContext) {
        OpenApiConstants.classLoader = Thread.currentThread().getContextClassLoader();
        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                OpenApiConstants.classLoader = null;
            }
        });
    }

    public void prepareDocument(AutoSecurityFilter autoSecurityFilter,
            Map<OpenApiFilter.RunStage, List<String>> filtersByStage, String documentName) {
        OpenApiDocumentService openApiDocumentService = Arc.container().select(OpenApiDocumentService.class).get();
        openApiDocumentService.prepareDocument(autoSecurityFilter, filtersByStage, documentName);
    }
}
