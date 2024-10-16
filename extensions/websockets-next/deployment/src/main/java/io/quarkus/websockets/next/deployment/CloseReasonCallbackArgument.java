package io.quarkus.websockets.next.deployment;

import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;

class CloseReasonCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        return context.callbackAnnotation().name().equals(WebSocketDotNames.ON_CLOSE)
                && context.parameter().type().name().equals(WebSocketDotNames.CLOSE_REASON);
    }

    @Override
    public ResultHandle get(InvocationBytecodeContext context) {
        return context.bytecode().invokeVirtualMethod(
                MethodDescriptor.ofMethod(WebSocketConnectionBase.class, "closeReason", CloseReason.class),
                context.getConnection());
    }

}
