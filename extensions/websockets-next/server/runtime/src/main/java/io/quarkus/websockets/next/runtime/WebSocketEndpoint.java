package io.quarkus.websockets.next.runtime;

import java.lang.reflect.Type;

import io.vertx.core.Context;
import io.vertx.core.Future;

/**
 * Internal representation of a WebSocket endpoint.
 * <p>
 * A new instance is created for each client connection.
 */
public interface WebSocketEndpoint {

    Future<Void> onOpen(Context context);

    default ExecutionModel onOpenExecutionModel() {
        return ExecutionModel.EVENT_LOOP;
    }

    Future<Void> onMessage(Context context, Object message);

    default ExecutionModel onMessageExecutionModel() {
        return ExecutionModel.EVENT_LOOP;
    }

    Future<Void> onClose(Context context);

    default ExecutionModel onCloseExecutionModel() {
        return ExecutionModel.EVENT_LOOP;
    }

    default MessageType consumedMessageType() {
        return MessageType.NONE;
    }

    default Type consumedMultiType() {
        return null;
    }

    enum ExecutionModel {
        WORKER_THREAD,
        VIRTUAL_THREAD,
        EVENT_LOOP
    }

    enum MessageType {
        NONE,
        TEXT,
        BINARY
    }
}
