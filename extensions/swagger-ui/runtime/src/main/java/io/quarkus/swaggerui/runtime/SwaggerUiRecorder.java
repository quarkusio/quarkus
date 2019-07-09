package io.quarkus.swaggerui.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.undertow.servlet.ServletExtension;

@Recorder
public class SwaggerUiRecorder {

    public ServletExtension createSwaggerUiExtension(String path, String resourceDir, BeanContainer container) {
        SwaggerUiServletExtension extension = container.instance(SwaggerUiServletExtension.class);
        extension.setPath(path);
        extension.setResourceDir(resourceDir);
        return extension;
    }
}
