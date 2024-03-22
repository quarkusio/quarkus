package io.quarkus.websockets.next.deployment;

import io.quarkus.gizmo.ResultHandle;

class ConnectionCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        return context.parameter().type().name().equals(WebSocketDotNames.WEB_SOCKET_CONNECTION);
    }

    @Override
    public ResultHandle get(InvocationBytecodeContext context) {
        return context.getConnection();
    }

}
