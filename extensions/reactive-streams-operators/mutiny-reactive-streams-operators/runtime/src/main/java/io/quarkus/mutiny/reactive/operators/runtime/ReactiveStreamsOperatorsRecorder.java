package io.quarkus.mutiny.reactive.operators.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.microprofile.reactive.streams.operators.core.ReactiveStreamsEngineResolver;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsFactoryResolver;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ReactiveStreamsOperatorsRecorder {

    /**
     * ClassLoader hack to work around reactive streams API issue
     * see https://github.com/eclipse/microprofile-reactive-streams-operators/pull/130
     *
     * This must be deleted when Reactive Streams Operators 1.1 is merged
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
            ReactiveStreamsFactoryResolver.instance();
            ReactiveStreamsEngineResolver.instance();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }

    }
}
