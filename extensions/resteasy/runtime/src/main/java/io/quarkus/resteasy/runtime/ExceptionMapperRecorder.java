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

}
