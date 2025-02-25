package io.quarkus.websockets.next.runtime.telemetry;

import java.lang.reflect.Type;

import io.quarkus.websockets.next.InboundProcessingMode;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

/**
 * {@link WebSocketEndpoint} wrapper that delegates all methods to {@link #delegate}.
 * This way, subclasses can only override methods they need to intercept.
 */
abstract class ForwardingWebSocketEndpoint implements WebSocketEndpoint {

    protected final WebSocketEndpoint delegate;

    protected ForwardingWebSocketEndpoint(WebSocketEndpoint delegate) {
        this.delegate = delegate;
    }

    @Override
    public InboundProcessingMode inboundProcessingMode() {
        return delegate.inboundProcessingMode();
    }

    @Override
    public Future<Void> onOpen() {
        return delegate.onOpen();
    }

    @Override
    public ExecutionModel onOpenExecutionModel() {
        return delegate.onOpenExecutionModel();
    }

    @Override
    public Future<Void> onTextMessage(Object message) {
        return delegate.onTextMessage(message);
    }

    @Override
    public ExecutionModel onTextMessageExecutionModel() {
        return delegate.onTextMessageExecutionModel();
    }

    @Override
    public Type consumedTextMultiType() {
        return delegate.consumedTextMultiType();
    }

    @Override
    public Object decodeTextMultiItem(Object message) {
        return delegate.decodeTextMultiItem(message);
    }

    @Override
    public Future<Void> onBinaryMessage(Object message) {
        return delegate.onBinaryMessage(message);
    }

    @Override
    public ExecutionModel onBinaryMessageExecutionModel() {
        return delegate.onBinaryMessageExecutionModel();
    }

    @Override
    public Type consumedBinaryMultiType() {
        return delegate.consumedBinaryMultiType();
    }

    @Override
    public Object decodeBinaryMultiItem(Object message) {
        return delegate.decodeBinaryMultiItem(message);
    }

    @Override
    public Future<Void> onPingMessage(Buffer message) {
        return delegate.onPingMessage(message);
    }

    @Override
    public ExecutionModel onPingMessageExecutionModel() {
        return delegate.onPingMessageExecutionModel();
    }

    @Override
    public Future<Void> onPongMessage(Buffer message) {
        return delegate.onPongMessage(message);
    }

    @Override
    public ExecutionModel onPongMessageExecutionModel() {
        return delegate.onPongMessageExecutionModel();
    }

    @Override
    public Future<Void> onClose() {
        return delegate.onClose();
    }

    @Override
    public ExecutionModel onCloseExecutionModel() {
        return delegate.onCloseExecutionModel();
    }

    @Override
    public Uni<Void> doOnError(Throwable t) {
        return delegate.doOnError(t);
    }

    @Override
    public String beanIdentifier() {
        return delegate.beanIdentifier();
    }
}
