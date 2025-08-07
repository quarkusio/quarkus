package io.quarkus.smallrye.openapi.runtime;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.microprofile.openapi.OASFilter;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
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

    public Handler<RoutingContext> handler() {
        if (openApiConfig.getValue().enable()) {
            return new OpenApiHandler();
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

    public Supplier<OASFilter> autoSecurityFilterSupplier(OASFilter autoSecurityFilter) {
        return new Supplier<>() {
            @Override
            public OASFilter get() {
                return autoSecurityFilter;
            }
        };
    }

    public Supplier<?> createUserDefinedRuntimeFilters(List<String> filters) {
        return new Supplier<Object>() {
            @Override
            public UserDefinedRuntimeFilters get() {
                return new UserDefinedRuntimeFilters() {
                    @Override
                    public List<String> filters() {
                        return filters;
                    }
                };
            }
        };
    }

    public interface UserDefinedRuntimeFilters {
        List<String> filters();
    }
}
