package io.quarkus.websockets.next.runtime;

import java.lang.reflect.Type;

import io.quarkus.websockets.next.InboundProcessingMode;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

/**
 * Internal representation of a WebSocket endpoint.
 * <p>
 * A new instance is created for each connection.
 */
public interface WebSocketEndpoint {

    /**
     *
     * @return the inbound processing mode
     */
    InboundProcessingMode inboundProcessingMode();

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

    // @OnPingMessage

    Future<Void> onPingMessage(Buffer message);

    default ExecutionModel onPingMessageExecutionModel() {
        return ExecutionModel.NONE;
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

    Uni<Void> doOnError(Throwable t);

    /**
     *
     * @return the identifier of the bean with callbacks
     * @see io.quarkus.arc.InjectableBean#getIdentifier()
     */
    String beanIdentifier();

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
