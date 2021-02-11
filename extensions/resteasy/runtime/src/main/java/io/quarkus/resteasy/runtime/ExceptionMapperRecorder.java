package io.quarkus.resteasy.runtime;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.devmode.AdditionalRouteDescription;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;

@Recorder
public class ExceptionMapperRecorder {

    public void setStaticResourceRoots(Set<String> resources) {
        NotFoundExceptionMapper.staticResources(resources);
    }

    public void setAdditionalEndpoints(List<AdditionalRouteDescription> additionalEndpoints) {
        NotFoundExceptionMapper.setAdditionalEndpoints(additionalEndpoints);
    }

    public void setReactiveRoutes(List<RouteDescription> reactiveRoutes) {
        NotFoundExceptionMapper.setReactiveRoutes(reactiveRoutes);
    }

    public void setServlets(Map<String, List<String>> servletToMapping) {
        NotFoundExceptionMapper.servlets(servletToMapping);
    }

    /**
     * Uses to register the paths of classes that are not annotated with JAX-RS annotations (like Spring Controllers for
     * example)
     *
     * @param nonJaxRsClassNameToMethodPaths A map that contains the class name as a key and a map that
     *        contains the method name to path as a value
     */
    public void nonJaxRsClassNameToMethodPaths(Map<String, NonJaxRsClassMappings> nonJaxRsClassNameToMethodPaths) {
        NotFoundExceptionMapper.nonJaxRsClassNameToMethodPaths(nonJaxRsClassNameToMethodPaths);
    }

    public void setHttpRoot(String rootPath) {
        NotFoundExceptionMapper.setHttpRoot(rootPath);
    }
}
