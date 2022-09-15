package org.jboss.resteasy.reactive.server.processor.util;

import jakarta.ws.rs.core.Context;
import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;

public class ResteasyReactiveServerDotNames {
    public static final DotName CONTEXT = DotName.createSimple(Context.class.getName());
    public static final DotName SERVER_REQUEST_FILTER = DotName
            .createSimple(ServerRequestFilter.class.getName());
    public static final DotName SERVER_RESPONSE_FILTER = DotName
            .createSimple(ServerResponseFilter.class.getName());
    public static final DotName SERVER_MESSAGE_BODY_WRITER = DotName
            .createSimple(ServerMessageBodyWriter.class.getName());
    public static final DotName SERVER_MESSAGE_BODY_WRITER_ALL_WRITER = DotName
            .createSimple(ServerMessageBodyWriter.AllWriteableMessageBodyWriter.class.getName());
    public static final DotName SERVER_MESSAGE_BODY_READER = DotName
            .createSimple(ServerMessageBodyReader.class.getName());
    public static final DotName QUARKUS_REST_CONTAINER_REQUEST_CONTEXT = DotName
            .createSimple(ResteasyReactiveContainerRequestContext.class.getName());
    public static final DotName SIMPLIFIED_RESOURCE_INFO = DotName.createSimple(SimpleResourceInfo.class.getName());

}
