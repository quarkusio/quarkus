package io.quarkus.rest.deployment.framework;

import org.jboss.jandex.DotName;

import io.quarkus.rest.server.runtime.spi.QuarkusRestContainerRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestContainerResponseContext;
import io.quarkus.rest.server.runtime.spi.SimplifiedResourceInfo;

public class QuarkusRestServerDotNames {
    public static final DotName QUARKUS_REST_CONTAINER_REQUEST_CONTEXT = DotName
            .createSimple(QuarkusRestContainerRequestContext.class.getName());
    public static final DotName QUARKUS_REST_CONTAINER_RESPONSE_CONTEXT = DotName
            .createSimple(QuarkusRestContainerResponseContext.class.getName());
    public static final DotName SIMPLIFIED_RESOURCE_INFO = DotName.createSimple(SimplifiedResourceInfo.class.getName());
}
