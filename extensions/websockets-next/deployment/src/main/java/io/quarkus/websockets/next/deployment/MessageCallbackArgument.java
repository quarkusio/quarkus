package io.quarkus.websockets.next.deployment;

import io.quarkus.gizmo2.Expr;

class MessageCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        return context.acceptsMessage();
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
