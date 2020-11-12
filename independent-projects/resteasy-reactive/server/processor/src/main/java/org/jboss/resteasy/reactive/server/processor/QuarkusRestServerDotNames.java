package org.jboss.resteasy.reactive.server.processor;

import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.server.spi.SimplifiedResourceInfo;

public class QuarkusRestServerDotNames {
    public static final DotName QUARKUS_REST_CONTAINER_REQUEST_CONTEXT = DotName
            .createSimple("org.jboss.resteasy.reactive.server.spi.QuarkusRestContainerRequestContext");
    public static final DotName QUARKUS_REST_CONTAINER_RESPONSE_CONTEXT = DotName
            .createSimple("org.jboss.resteasy.reactive.server.spi.QuarkusRestContainerResponseContext");
    public static final DotName SIMPLIFIED_RESOURCE_INFO = DotName.createSimple(SimplifiedResourceInfo.class.getName());
    //public static final DotName ROUTING_CONTEXT = DotName.createSimple(RoutingContext.class.getName());
}
