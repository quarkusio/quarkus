package io.quarkus.websockets.next.runtime.telemetry;

import io.quarkus.websockets.next.runtime.WebSocketEndpoint;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketMetricsInterceptorProducer.WebSocketMetricsInterceptor;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

final class MetricsForwardingWebSocketEndpoint extends ForwardingWebSocketEndpoint {

    private final WebSocketMetricsInterceptor interceptor;
    private final String path;

    MetricsForwardingWebSocketEndpoint(WebSocketEndpoint delegate, WebSocketMetricsInterceptor interceptor, String path) {
        super(delegate);
        this.interceptor = interceptor;
        this.path = path;
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
        interceptor.onConnectionClosed(path);
        return delegate.onClose();
    }

    private void addMetricsIfMessageIsString(Object message) {
        if (message instanceof String stringMessage) {
            interceptor.onMessageReceived(stringMessage.getBytes(), path);
        }
    }

    private void addMetricsIfMessageIsBuffer(Object message) {
        if (message instanceof Buffer bufferMessage) {
            interceptor.onMessageReceived(bufferMessage.getBytes(), path);
        }
    }
}
