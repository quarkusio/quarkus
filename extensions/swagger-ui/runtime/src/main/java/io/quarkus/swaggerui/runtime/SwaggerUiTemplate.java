package io.quarkus.swaggerui.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;
import io.undertow.servlet.ServletExtension;

@Template
public class SwaggerUiTemplate {

    public ServletExtension createSwaggerUiExtension(String path, String resourceDir, BeanContainer container) {
        SwaggerUiServletExtension extension = container.instance(SwaggerUiServletExtension.class);
        extension.setPath(path);
        extension.setResourceDir(resourceDir);
        return extension;
    }
}
