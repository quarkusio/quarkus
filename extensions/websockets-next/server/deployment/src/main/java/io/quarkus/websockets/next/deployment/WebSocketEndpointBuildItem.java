package io.quarkus.websockets.next.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint.ExecutionModel;

/**
 * This build item represents a WebSocket endpoint class.
 */
public final class WebSocketEndpointBuildItem extends MultiBuildItem {

    public final BeanInfo bean;
    public final String path;
    public final WebSocket.ExecutionMode executionMode;
    public final Callback onOpen;
    public final Callback onTextMessage;
    public final Callback onBinaryMessage;
    public final Callback onPongMessage;
    public final Callback onClose;

    public WebSocketEndpointBuildItem(BeanInfo bean, String path, WebSocket.ExecutionMode executionMode, Callback onOpen,
            Callback onTextMessage, Callback onBinaryMessage, Callback onPongMessage, Callback onClose) {
        this.bean = bean;
        this.path = path;
        this.executionMode = executionMode;
        this.onOpen = onOpen;
        this.onTextMessage = onTextMessage;
        this.onBinaryMessage = onBinaryMessage;
        this.onPongMessage = onPongMessage;
        this.onClose = onClose;
    }

    public static class Callback {

        public final AnnotationInstance annotation;
        public final MethodInfo method;
        public final ExecutionModel executionModel;
        public final MessageType messageType;

        public Callback(AnnotationInstance annotation, MethodInfo method, ExecutionModel executionModel) {
            this.method = method;
            this.annotation = annotation;
            this.executionModel = executionModel;
            if (WebSocketDotNames.ON_BINARY_MESSAGE.equals(annotation.name())) {
                this.messageType = MessageType.BINARY;
            } else if (WebSocketDotNames.ON_TEXT_MESSAGE.equals(annotation.name())) {
                this.messageType = MessageType.TEXT;
            } else if (WebSocketDotNames.ON_PONG_MESSAGE.equals(annotation.name())) {
                this.messageType = MessageType.PONG;
            } else {
                this.messageType = MessageType.NONE;
            }
        }

        public Type returnType() {
            return method.returnType();
        }

        public Type messageParamType() {
            return acceptsMessage() ? method.parameterType(0) : null;
        }

        public boolean isReturnTypeVoid() {
            return returnType().kind() == Kind.VOID;
        }

        public boolean isReturnTypeUni() {
            return WebSocketDotNames.UNI.equals(returnType().name());
        }

        public boolean isReturnTypeMulti() {
            return WebSocketDotNames.MULTI.equals(returnType().name());
        }

        public boolean acceptsMessage() {
            return messageType != MessageType.NONE;
        }

        public boolean acceptsBinaryMessage() {
            return messageType == MessageType.BINARY || messageType == MessageType.PONG;
        }

        public boolean acceptsMulti() {
            return acceptsMessage() && method.parameterType(0).name().equals(WebSocketDotNames.MULTI);
        }

        public MessageType messageType() {
            return messageType;
        }

        public boolean broadcast() {
            AnnotationValue broadcastValue = annotation.value("broadcast");
            return broadcastValue != null && broadcastValue.asBoolean();
        }

        public DotName getInputCodec() {
            return getCodec("codec");
        }

        public DotName getOutputCodec() {
            DotName output = getCodec("outputCodec");
            return output != null ? output : getInputCodec();
        }

        private DotName getCodec(String valueName) {
            AnnotationValue codecValue = annotation.value(valueName);
            if (codecValue != null) {
                return codecValue.asClass().name();
            }
            return null;
        }

        enum MessageType {
            NONE,
            PONG,
            TEXT,
            BINARY
        }

    }

}
