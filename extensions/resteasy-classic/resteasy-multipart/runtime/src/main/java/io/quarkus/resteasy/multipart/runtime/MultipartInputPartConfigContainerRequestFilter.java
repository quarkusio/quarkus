package io.quarkus.resteasy.multipart.runtime;

import java.io.IOException;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;

import io.quarkus.arc.WithCaching;

@Provider
public class MultipartInputPartConfigContainerRequestFilter implements ContainerRequestFilter {

    @WithCaching
    @Inject
    Instance<ResteasyMultipartRuntimeConfig> resteasyMultipartConfigInstance;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ResteasyMultipartRuntimeConfig resteasyMultipartConfig = resteasyMultipartConfigInstance.get();

        requestContext.setProperty(InputPart.DEFAULT_CHARSET_PROPERTY,
                resteasyMultipartConfig.inputPart.defaultCharset.name());
        requestContext.setProperty(InputPart.DEFAULT_CONTENT_TYPE_PROPERTY,
                resteasyMultipartConfig.inputPart.defaultContentType);
    }
}
