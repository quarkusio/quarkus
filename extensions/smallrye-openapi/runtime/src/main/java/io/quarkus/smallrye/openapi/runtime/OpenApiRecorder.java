package io.quarkus.smallrye.openapi.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.filters.Filter;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class OpenApiRecorder {
    private static final Logger log = Logger.getLogger(OpenApiRecorder.class);
    final RuntimeValue<HttpConfiguration> configuration;

    public OpenApiRecorder(RuntimeValue<HttpConfiguration> configuration) {
        this.configuration = configuration;
    }

    public Consumer<Route> corsFilter(Filter filter) {
        if (configuration.getValue().corsEnabled && filter.getHandler() != null) {
            return new Consumer<Route>() {
                @Override
                public void accept(Route route) {
                    route.order(-1 * filter.getPriority()).handler(filter.getHandler());
                }
            };
        }
        return null;
    }

    public Handler<RoutingContext> handler(OpenApiRuntimeConfig runtimeConfig) {
        if (runtimeConfig.enable) {
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

    /**
     * ClassLoader hack to work around reactive streams API issue
     * see https://github.com/eclipse/microprofile-open-api/pull/470
     * <p>
     * This must be deleted when it is fixed upstream
     */
    public void classLoaderHack() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(new ClassLoader(null) {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    return cl.loadClass(name);
                }

                @Override
                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    return cl.loadClass(name);
                }

                @Override
                public URL getResource(String name) {
                    return cl.getResource(name);
                }

                @Override
                public Enumeration<URL> getResources(String name) throws IOException {
                    return cl.getResources(name);
                }

                @Override
                public InputStream getResourceAsStream(String name) {
                    return cl.getResourceAsStream(name);
                }
            });
            OASFactoryResolver.instance();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }

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
