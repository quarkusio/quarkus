package io.quarkus.resteasy.runtime;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ExceptionMapperRecorder {

    public void setStaticResource(Set<String> resources) {
        NotFoundExceptionMapper.staticResources(resources);
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

}
