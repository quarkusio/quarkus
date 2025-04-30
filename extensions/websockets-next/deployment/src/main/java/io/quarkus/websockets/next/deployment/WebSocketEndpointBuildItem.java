package io.quarkus.websockets.next.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.websockets.next.InboundProcessingMode;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClient;

/**
 * This build item represents a WebSocket endpoint class, i.e. class annotated with {@link WebSocket} or
 * {@link WebSocketClient}.
 */
public final class WebSocketEndpointBuildItem extends MultiBuildItem {

    public final boolean isClient;
    public final BeanInfo bean;
    // The path is using Vertx syntax for path params, i.e. /foo/:bar
    public final String path;
    // @WebSocket#endpointId() or @WebSocketClient#clientId()
    public final String id;
    public final InboundProcessingMode inboundProcessingMode;
    public final Callback onOpen;
    public final Callback onTextMessage;
    public final Callback onBinaryMessage;
    public final Callback onPingMessage;
    public final Callback onPongMessage;
    public final Callback onClose;
    public final List<Callback> onErrors;

    WebSocketEndpointBuildItem(boolean isClient, BeanInfo bean, String path, String id,
            InboundProcessingMode inboundProcessingMode,
            Callback onOpen, Callback onTextMessage, Callback onBinaryMessage, Callback onPingMessage,
            Callback onPongMessage, Callback onClose, List<Callback> onErrors) {
        this.isClient = isClient;
        this.bean = bean;
        this.path = path;
        this.id = id;
        this.inboundProcessingMode = inboundProcessingMode;
        this.onOpen = onOpen;
        this.onTextMessage = onTextMessage;
        this.onBinaryMessage = onBinaryMessage;
        this.onPingMessage = onPingMessage;
        this.onPongMessage = onPongMessage;
        this.onClose = onClose;
        this.onErrors = onErrors;
    }

    public boolean isClient() {
        return isClient;
    }

    public boolean isServer() {
        return !isClient;
    }

    public DotName beanClassName() {
        return bean.getImplClazz().name();
    }

}
