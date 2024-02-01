package io.quarkus.websockets.next.runtime;

import java.lang.reflect.Type;

import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.WebSocket;
import io.vertx.core.Future;

/**
 * Internal representation of a WebSocket endpoint.
 * <p>
 * A new instance is created for each client connection. {@link #onOpen()}, {@link #onMessage(Object)} and {@link OnClose} are
 * always executed on a new vertx duplicated context.
 */
public interface WebSocketEndpoint {

    WebSocket.ExecutionMode executionMode();

    Future<Void> onOpen();

    default ExecutionModel onOpenExecutionModel() {
        return ExecutionModel.NONE;
    }

    Future<Void> onMessage(Object message);

    default ExecutionModel onMessageExecutionModel() {
        return ExecutionModel.NONE;
    }

    Future<Void> onClose();

    default ExecutionModel onCloseExecutionModel() {
        return ExecutionModel.NONE;
    }

    default MessageType consumedMessageType() {
        return MessageType.NONE;
    }

    default Type consumedMultiType() {
        return null;
    }

    default Object decodeMultiItem(Object message) {
        throw new UnsupportedOperationException();
    }

    enum ExecutionModel {
        WORKER_THREAD,
        VIRTUAL_THREAD,
        EVENT_LOOP,
        NONE;

        boolean isBlocking() {
            return this == WORKER_THREAD || this == VIRTUAL_THREAD;
        }
    }

    enum MessageType {
        NONE,
        TEXT,
        BINARY
    }
}
