package io.quarkus.websockets.next.runtime.telemetry;

import io.micrometer.core.instrument.Counter;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

final class MetricsForwardingWebSocketEndpoint extends ForwardingWebSocketEndpoint {

    private final Counter onMessageReceivedCounter;
    private final Counter onMessageReceivedBytesCounter;
    private final Counter onConnectionClosedCounter;

    MetricsForwardingWebSocketEndpoint(WebSocketEndpoint delegate, Counter onMessageReceivedCounter,
            Counter onMessageReceivedBytesCounter, Counter onConnectionClosedCounter) {
        super(delegate);
        this.onMessageReceivedCounter = onMessageReceivedCounter;
        this.onMessageReceivedBytesCounter = onMessageReceivedBytesCounter;
        this.onConnectionClosedCounter = onConnectionClosedCounter;
    }

    @Override
    public Future<Void> onTextMessage(Object message) {
        addMetricsIfMessageIsString(message);
        return delegate.onTextMessage(message);
    }

    @Override
    public Future<Void> onBinaryMessage(Object message) {
        addMetricsIfMessageIsBuffer(message);
        return delegate.onBinaryMessage(message);
    }

    @Override
    public Object decodeTextMultiItem(Object message) {
        addMetricsIfMessageIsString(message);
        return delegate.decodeTextMultiItem(message);
    }

    @Override
    public Object decodeBinaryMultiItem(Object message) {
        addMetricsIfMessageIsBuffer(message);
        return delegate.decodeBinaryMultiItem(message);
    }

    @Override
    public Future<Void> onClose() {
        onConnectionClosedCounter.increment();
        return delegate.onClose();
    }

    private void addMetricsIfMessageIsString(Object message) {
        if (message instanceof String stringMessage) {
            onMessageReceivedCounter.increment();
            double bytesNum = stringMessage.getBytes().length;
            onMessageReceivedBytesCounter.increment(bytesNum);
        }
    }

    private void addMetricsIfMessageIsBuffer(Object message) {
        if (message instanceof Buffer bufferMessage) {
            onMessageReceivedCounter.increment();
            double bytesNum = bufferMessage.getBytes().length;
            onMessageReceivedBytesCounter.increment(bytesNum);
        }
    }
}
