package io.quarkus.websockets.next.deployment;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;

class CloseReasonCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        return context.callbackAnnotation().name().equals(WebSocketDotNames.ON_CLOSE)
                && context.parameter().type().name().equals(WebSocketDotNames.CLOSE_REASON);
    }

    @Override
    public Expr get(InvocationBytecodeContext context) {
        return context.bytecode().invokeVirtual(
                MethodDesc.of(WebSocketConnectionBase.class, "closeReason", CloseReason.class),
                context.getConnection());
    }

}
