package io.quarkus.websockets.next.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.gizmo2.Expr;

class MessageCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        if (!context.acceptsMessage()) {
            return false;
        }
        if (context.parameterAnnotations().isEmpty()) {
            return true;
        }
        // allow OpenTelemetry @SpanAttribute annotation on the method callback argument
        return context.parameterAnnotations().stream().map(AnnotationInstance::name).map(DotName::toString)
                .allMatch(WebSocketConstants.OPEN_TELEMETRY_SPAN_ATTRIBUTE::equals);
    }

    @Override
    public Expr get(InvocationBytecodeContext context) {
        return context.getDecodedMessage(context.parameter().type());
    }

    @Override
    public int priority() {
        return DEFAULT_PRIORITY - 1;
    }

    public static boolean isMessage(CallbackArgument callbackArgument) {
        return callbackArgument instanceof MessageCallbackArgument;
    }

}
