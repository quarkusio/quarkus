package io.quarkus.websockets.next.runtime;

import java.lang.reflect.Type;

import io.quarkus.websockets.next.WebSocket;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

/**
 * Internal representation of a WebSocket endpoint.
 * <p>
 * A new instance is created for each client connection.
 */
public interface WebSocketEndpoint {

    /**
     *
     * @see WebSocket#executionMode()
     * @return the execution mode
     */
    WebSocket.ExecutionMode executionMode();

    // @OnOpen

    Future<Void> onOpen();

    default ExecutionModel onOpenExecutionModel() {
        return ExecutionModel.NONE;
    }

    // @OnTextMessage

    Future<Void> onTextMessage(Object message);

    default ExecutionModel onTextMessageExecutionModel() {
        return ExecutionModel.NONE;
    }

    default Type consumedTextMultiType() {
        return null;
    }

    default Object decodeTextMultiItem(Object message) {
        throw new UnsupportedOperationException();
    }

    // @OnBinaryMessage

    Future<Void> onBinaryMessage(Object message);

    default ExecutionModel onBinaryMessageExecutionModel() {
        return ExecutionModel.NONE;
    }

    default Type consumedBinaryMultiType() {
        return null;
    }

    default Object decodeBinaryMultiItem(Object message) {
        throw new UnsupportedOperationException();
    }

    // @OnPongMessage

    Future<Void> onPongMessage(Buffer message);

    default ExecutionModel onPongMessageExecutionModel() {
        return ExecutionModel.NONE;
    }

    // @OnClose

    Future<Void> onClose();

    default ExecutionModel onCloseExecutionModel() {
        return ExecutionModel.NONE;
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

}
