package io.quarkus.devui.runtime.mcp;

import io.vertx.core.json.JsonObject;

public class JsonRPC {

    public static final String VERSION = "2.0";

    public static final int RESOURCE_NOT_FOUND = -32002;

    public static final int INTERNAL_ERROR = -32603;
    public static final int INVALID_PARAMS = -32602;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_REQUEST = -32600;
    public static final int PARSE_ERROR = -32700;

    public static final int SECURITY_ERROR = -32001;

    public static boolean validate(JsonObject message, Sender sender) {
        Object id = message.getValue("id");
        String jsonrpc = message.getString("jsonrpc");
        if (!VERSION.equals(jsonrpc)) {
            sender.sendError(id, INVALID_REQUEST, "Invalid jsonrpc version: " + jsonrpc);
            return false;
        }
        if (!Messages.isResponse(message)) {
            if (message.getString("method") == null) {
                sender.sendError(id, METHOD_NOT_FOUND, "Method not set");
                return false;
            }
        }
        return true;
    }

}
