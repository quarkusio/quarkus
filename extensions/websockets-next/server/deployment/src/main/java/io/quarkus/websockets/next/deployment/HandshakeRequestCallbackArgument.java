package io.quarkus.websockets.next.deployment;

import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.websockets.next.WebSocketConnection;

class HandshakeRequestCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        return context.parameter().type().name().equals(WebSocketDotNames.HANDSHAKE_REQUEST);
    }

    @Override
    public ResultHandle get(InvocationBytecodeContext context) {
        ResultHandle connection = context.getConnection();
        return context.bytecode().invokeInterfaceMethod(MethodDescriptor.ofMethod(WebSocketConnection.class, "handshakeRequest",
                WebSocketConnection.HandshakeRequest.class), connection);
    }

}
