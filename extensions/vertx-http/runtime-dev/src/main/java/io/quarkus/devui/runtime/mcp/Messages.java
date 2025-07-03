package io.quarkus.devui.runtime.mcp;

import org.jboss.logging.Logger;

import io.vertx.core.json.JsonObject;

public class Messages {

    private static final Logger LOG = Logger.getLogger(Messages.class);

    public static JsonObject newResult(Object id, Object result) {
        JsonObject response = new JsonObject();
        response.put("jsonrpc", JsonRPC.VERSION);
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    public static JsonObject newError(Object id, int code, String message) {
        JsonObject response = new JsonObject();
        response.put("jsonrpc", JsonRPC.VERSION);
        response.put("id", id);
        response.put("error", new JsonObject()
                .put("code", code)
                .put("message", message));
        return response;
    }

    public static JsonObject newNotification(String method, Object params) {
        JsonObject ret = new JsonObject()
                .put("jsonrpc", JsonRPC.VERSION)
                .put("method", method);
        if (params != null) {
            ret.put("params", params);
        }
        return ret;
    }

    public static JsonObject newNotification(String method) {
        return newNotification(method, null);
    }

    public static JsonObject newPing(Object id) {
        return newRequest(id, "ping");
    }

    public static JsonObject newRequest(Object id, String method) {
        return newRequest(id, method, null);
    }

    public static JsonObject newRequest(Object id, String method, Object params) {
        JsonObject request = new JsonObject();
        request.put("jsonrpc", JsonRPC.VERSION);
        request.put("id", id);
        request.put("method", method);
        if (params != null) {
            request.put("params", params);
        }
        return request;
    }

    public static boolean isResponse(JsonObject message) {
        return message.containsKey("result") || message.containsKey("error");
    }

    public static boolean isRequest(JsonObject message) {
        return !isResponse(message) && message.getValue("id") != null;
    }

    public static boolean isNotification(JsonObject message) {
        return !isResponse(message) && message.getValue("id") == null;
    }

    static Cursor getCursor(JsonObject message, Sender sender) {
        JsonObject params = message.getJsonObject("params");
        if (params != null) {
            String cursorVal = params.getString("cursor");
            if (cursorVal != null) {
                try {
                    return Cursor.decode(cursorVal);
                } catch (Exception e) {
                    // Invalid cursors should result in an error with code -32602 (Invalid params).
                    LOG.warnf("Invalid cursor detected %s: %s", cursorVal, e.toString());
                    sender.sendError(message.getValue("id"), JsonRPC.INVALID_PARAMS,
                            "Invalid cursor detected: " + cursorVal);
                    return null;
                }
            }
        }
        return Cursor.FIRST_PAGE;
    }

    static Object getProgressToken(JsonObject message) {
        JsonObject params = message.getJsonObject("params");
        if (params != null) {
            JsonObject meta = params.getJsonObject("_meta");
            if (meta != null) {
                return meta.getValue("progressToken");
            }
        }
        return null;
    }

}
