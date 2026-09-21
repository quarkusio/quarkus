package io.quarkus.resteasy.reactive.server.runtime;

import java.util.Map;

import org.jboss.resteasy.reactive.common.jaxrs.ResourceMethodPathRegistry;
import org.jboss.resteasy.reactive.common.jaxrs.UriBuilderImpl;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.StaticInit;

@Recorder
public class ResourceMethodPathRecorder {

    @StaticInit
    public void setResourceMethodPaths(Map<String, Map<String, String>> paths) {
        ResourceMethodPathRegistry.setResourceMethodPaths(paths);
    }

    @StaticInit
    public void cleanUp(ShutdownContext shutdown) {
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                ResourceMethodPathRegistry.clear();
                UriBuilderImpl.clearMethodCache();
            }
        });
    }
}
