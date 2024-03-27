package io.quarkus.websockets.next.deployment;

import io.quarkus.gizmo.ResultHandle;

class MessageCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        return context.acceptsMessage() && context.parameterAnnotations().isEmpty();
    }

    @Override
    public ResultHandle get(InvocationBytecodeContext context) {
        return context.getDecodedMessage(context.parameter().type());
    }

    @Override
    public int priotity() {
        return DEFAULT_PRIORITY - 1;
    }

    public static boolean isMessage(CallbackArgument callbackArgument) {
        return callbackArgument instanceof MessageCallbackArgument;
    }

}
