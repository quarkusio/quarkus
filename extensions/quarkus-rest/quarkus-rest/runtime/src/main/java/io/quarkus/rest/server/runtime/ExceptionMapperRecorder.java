package io.quarkus.rest.server.runtime;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;

@Recorder
public class ExceptionMapperRecorder {

    public void setStaticResourceRoots(Set<String> resources) {
        NotFoundExceptionMapper.staticResources(resources);
    }

    public void setAdditionalEndpoints(List<String> additionalEndpoints) {
        NotFoundExceptionMapper.setAdditionalEndpoints(additionalEndpoints);
    }

    public void setReactiveRoutes(List<RouteDescription> reactiveRoutes) {
        NotFoundExceptionMapper.setReactiveRoutes(reactiveRoutes);
    }

    public void setServlets(Map<String, List<String>> servletToMapping) {
        NotFoundExceptionMapper.servlets(servletToMapping);
    }

    public void setHttpRoot(String rootPath) {
        NotFoundExceptionMapper.setHttpRoot(rootPath);
    }
}
