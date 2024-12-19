package io.quarkus.websockets.next.deployment;

import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketException;
import io.quarkus.websockets.next.deployment.Callback.Target;

/**
 * Provides arguments for method parameters of a callback method declared on a WebSocket endpoint.
 */
interface CallbackArgument {

    /**
     *
     * @param context
     * @return {@code true} if this provider matches the given parameter context, {@code false} otherwise
     * @throws WebSocketException If an invalid parameter is detected
     */
    boolean matches(ParameterContext context);

    /**
     * This method is only used if {@link #matches(ParameterContext)} previously returned {@code true} for the same parameter
     * context.
     *
     * @param context
     * @return the result handle to be passed as an argument to a callback method
     */
    ResultHandle get(InvocationBytecodeContext context);

    /**
     *
     * @return the priority
     */
    default int priotity() {
        return DEFAULT_PRIORITY;
    }

    static final int DEFAULT_PRIORITY = 1;

    interface ParameterContext {

        /**
         *
         * @return the callback target
         */
        Target callbackTarget();

        /**
         *
         * @return the endpoint path or {@code null} for global error handlers
         */
        String endpointPath();

        /**
         *
         * @return the index that can be used to inspect parameter types
         */
        IndexView index();

        /**
         *
         * @return the callback marker annotation
         */
        AnnotationInstance callbackAnnotation();

        /**
         *
         * @return the Java method parameter
         */
        MethodParameterInfo parameter();

        /**
         *
         * @return the set of parameter annotations, potentially transformed
         */
        Set<AnnotationInstance> parameterAnnotations();

        default boolean acceptsMessage() {
            return WebSocketDotNames.ON_BINARY_MESSAGE.equals(callbackAnnotation().name())
                    || WebSocketDotNames.ON_TEXT_MESSAGE.equals(callbackAnnotation().name())
                    || WebSocketDotNames.ON_PING_MESSAGE.equals(callbackAnnotation().name())
                    || WebSocketDotNames.ON_PONG_MESSAGE.equals(callbackAnnotation().name());
        }

    }

    interface InvocationBytecodeContext extends ParameterContext {

        /**
         *
         * @return the bytecode
         */
        BytecodeCreator bytecode();

        /**
         * Obtains the message or error directly in the bytecode.
         *
         * @return the message/error object or {@code null} for {@link OnOpen} and {@link OnClose} callbacks
         */
        ResultHandle getPayload();

        /**
         * Attempts to obtain the decoded message directly in the bytecode.
         *
         * @param parameterType
         * @return the decoded message object or {@code null} for {@link OnOpen}, {@link OnClose} and {@link OnError} callbacks
         */
        ResultHandle getDecodedMessage(Type parameterType);

        /**
         * Obtains the current connection directly in the bytecode.
         *
         * @return the current {@link WebSocketConnection}, never {@code null}
         */
        ResultHandle getConnection();

    }

}
