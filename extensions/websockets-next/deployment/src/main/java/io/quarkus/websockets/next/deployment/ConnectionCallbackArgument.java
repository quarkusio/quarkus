package io.quarkus.websockets.next.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.gizmo.ResultHandle;
import io.quarkus.websockets.next.WebSocketException;
import io.quarkus.websockets.next.deployment.Callback.Target;

class ConnectionCallbackArgument implements CallbackArgument {

    @Override
    public boolean matches(ParameterContext context) {
        DotName paramTypeName = context.parameter().type().name();
        if (context.callbackTarget() == Target.SERVER) {
            if (WebSocketDotNames.WEB_SOCKET_CONNECTION.equals(paramTypeName)
                    || WebSocketDotNames.CONNECTION.equals(paramTypeName)) {
                return true;
            } else if (WebSocketDotNames.WEB_SOCKET_CLIENT_CONNECTION.equals(paramTypeName)) {
                throw new WebSocketException("@WebSocket callback method may not accept WebSocketClientConnection");
            }
        } else if (context.callbackTarget() == Target.CLIENT) {
            if (WebSocketDotNames.WEB_SOCKET_CLIENT_CONNECTION.equals(paramTypeName)
                    || WebSocketDotNames.CONNECTION.equals(paramTypeName)) {
                return true;
            } else if (WebSocketDotNames.WEB_SOCKET_CONNECTION.equals(paramTypeName)) {
                throw new WebSocketException("@WebSocketClient callback method may not accept WebSocketConnection");
            }
        }
        return false;
    }

    @Override
    public ResultHandle get(InvocationBytecodeContext context) {
        return context.getConnection();
    }

}
