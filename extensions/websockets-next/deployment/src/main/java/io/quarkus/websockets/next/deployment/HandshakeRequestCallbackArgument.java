package io.quarkus.websockets.next.deployment;

import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.runtime.WebSocketConnectionBase;

class HandshakeRequestCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        return context.parameter().type().name().equals(WebSocketDotNames.HANDSHAKE_REQUEST);
    }

    @Override
    public Expr get(InvocationBytecodeContext context) {
        return context.bytecode().invokeVirtual(
                MethodDesc.of(WebSocketConnectionBase.class, "handshakeRequest", HandshakeRequest.class),
                context.getConnection());
    }

}
