package io.quarkus.websockets.next.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint.ExecutionModel;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint.MessageType;

/**
 * This build item represents a WebSocket endpoint class.
 */
public final class WebSocketEndpointBuildItem extends MultiBuildItem {

    public final BeanInfo bean;
    public final String path;
    public final Callback onOpen;
    public final Callback onMessage;
    public final Callback onClose;

    public WebSocketEndpointBuildItem(BeanInfo bean, String path, Callback onOpen, Callback onMessage, Callback onClose) {
        this.bean = bean;
        this.path = path;
        this.onOpen = onOpen;
        this.onMessage = onMessage;
        this.onClose = onClose;
    }

    public static class Callback {

        public final AnnotationInstance annotation;
        public final MethodInfo method;
        public final ExecutionModel executionModel;
        public final MessageType consumedMessageType;
        public final MessageType producedMessageType;

        public Callback(AnnotationInstance annotation, MethodInfo method, ExecutionModel executionModel) {
            this.method = method;
            this.annotation = annotation;
            this.executionModel = executionModel;
            this.consumedMessageType = initMessageType(method.parameters().isEmpty() ? null : method.parameterType(0));
            this.producedMessageType = initMessageType(method.returnType());
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
            return consumedMessageType != MessageType.NONE;
        }

        public boolean acceptsBinaryMessage() {
            return consumedMessageType == MessageType.BINARY;
        }

        public boolean acceptsMulti() {
            return acceptsMessage() && method.parameterType(0).name().equals(WebSocketDotNames.MULTI);
        }

        public WebSocketEndpoint.MessageType consumedMessageType() {
            return consumedMessageType;
        }

        public WebSocketEndpoint.MessageType producedMessageType() {
            return producedMessageType;
        }

        public boolean broadcast() {
            AnnotationValue broadcastValue = annotation.value("broadcast");
            return broadcastValue != null && broadcastValue.asBoolean();
        }

        public DotName getInputCodec() {
            return getCodec("inputCodec");
        }

        public DotName getOutpuCodec() {
            DotName output = getCodec("outputCodec");
            return output != null ? output : getInputCodec();
        }

        private DotName getCodec(String valueName) {
            AnnotationInstance messageAnnotation = method.declaredAnnotation(WebSocketDotNames.BINARY_MESSAGE);
            if (messageAnnotation == null) {
                messageAnnotation = method.declaredAnnotation(WebSocketDotNames.TEXT_MESSAGE);
            }
            if (messageAnnotation != null) {
                AnnotationValue codecValue = messageAnnotation.value(valueName);
                if (codecValue != null) {
                    return codecValue.asClass().name();
                }
            }
            return null;
        }

        MessageType initMessageType(Type messageType) {
            MessageType ret = MessageType.NONE;
            if (messageType != null && !messageType.name().equals(WebSocketDotNames.VOID)) {
                if (method.hasDeclaredAnnotation(WebSocketDotNames.BINARY_MESSAGE)) {
                    ret = MessageType.BINARY;
                } else if (method.hasDeclaredAnnotation(WebSocketDotNames.TEXT_MESSAGE)) {
                    ret = MessageType.TEXT;
                } else {
                    if (isByteArray(messageType) || WebSocketDotNames.BUFFER.equals(messageType.name())) {
                        ret = MessageType.BINARY;
                    } else {
                        ret = MessageType.TEXT;
                    }
                }
            }
            return ret;
        }

        static boolean isByteArray(Type type) {
            return type.kind() == Kind.ARRAY && PrimitiveType.BYTE.equals(type.asArrayType().constituent());
        }

        static boolean isUniVoid(Type type) {
            return WebSocketDotNames.UNI.equals(type.name())
                    && type.asParameterizedType().arguments().get(0).name().equals(WebSocketDotNames.VOID);
        }

    }

}
