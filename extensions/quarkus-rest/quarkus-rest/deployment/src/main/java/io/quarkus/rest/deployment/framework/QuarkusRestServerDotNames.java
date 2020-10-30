package io.quarkus.rest.deployment.framework;

import org.jboss.jandex.DotName;

import io.quarkus.rest.server.runtime.spi.SimplifiedResourceInfo;

public class QuarkusRestServerDotNames {
    public static final DotName QUARKUS_REST_CONTAINER_REQUEST_CONTEXT = DotName
            .createSimple("io.quarkus.rest.server.runtime.spi.QuarkusRestContainerRequestContext");
    public static final DotName QUARKUS_REST_CONTAINER_RESPONSE_CONTEXT = DotName
            .createSimple("io.quarkus.rest.server.runtime.spi.QuarkusRestContainerResponseContext");
    public static final DotName SIMPLIFIED_RESOURCE_INFO = DotName.createSimple(SimplifiedResourceInfo.class.getName());
}
