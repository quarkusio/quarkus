package io.quarkus.smallrye.openapi.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class OpenApiRecorder {

    public Handler<RoutingContext> handler(OpenApiRuntimeConfig runtimeConfig, HttpConfiguration configuration,
            OASFilter autoSecurityFilter) {
        if (runtimeConfig.enable) {
            return new OpenApiHandler(configuration.corsEnabled, autoSecurityFilter);
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
}
