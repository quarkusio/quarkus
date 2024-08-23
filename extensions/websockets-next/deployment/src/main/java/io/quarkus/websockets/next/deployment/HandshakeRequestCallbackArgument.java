package io.quarkus.websockets.next.deployment;

import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;

class HandshakeRequestCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        return context.parameter().type().name().equals(WebSocketDotNames.HANDSHAKE_REQUEST);
    }

    @Override
    public ResultHandle get(InvocationBytecodeContext context) {
        return context.bytecode()
                .invokeVirtualMethod(MethodDescriptor.ofMethod(WebSocketConnectionBase.class, "handshakeRequest",
                        HandshakeRequest.class), context.getConnection());
    }

}
