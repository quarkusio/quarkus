package io.quarkus.devui.runtime.mcp;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

interface Sender {

    Future<Void> send(JsonObject message);

    default Future<Void> sendResult(Object id, Object result) {
        return send(Messages.newResult(id, result));
    }

    default Future<Void> sendError(Object id, int code, String message) {
        return send(Messages.newError(id, code, message));
    }

    default Future<Void> sendInternalError(Object id) {
        return sendError(id, JsonRPC.INTERNAL_ERROR, "Internal error");
    }

}
