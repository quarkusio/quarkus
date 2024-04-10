package io.quarkus.grpc.transcoding;

import io.vertx.grpc.common.GrpcMessageEncoder;
import io.vertx.grpc.common.GrpcWriteStream;

/**
 * A write stream adapter that uses a {@link GrpcMessageEncoder} to encode the message payload.
 *
 * @param <T> The type of the message payload.
 * @see io.vertx.grpc.common.impl.WriteStreamAdapter for the original implementation
 */
public class GrpcTranscodingWriteStreamAdapter<T> {

    private GrpcWriteStream<T> stream;
    private boolean ready;
    private GrpcMessageEncoder<T> encoder;

    /**
     * Override this method to call gRPC {@code onReady}
     */
    protected void handleReady() {
    }

    public final void init(GrpcWriteStream<T> stream, GrpcTranscodingMessageEncoder<T> encoder) {
        synchronized (this) {
            this.stream = stream;
            this.encoder = encoder;
        }
        stream.drainHandler(v -> {
            checkReady();
        });
        checkReady();
    }

    public final synchronized boolean isReady() {
        return ready;
    }

    public final void write(T msg) {
        stream.writeMessage(encoder.encode(msg));
        synchronized (this) {
            ready = !stream.writeQueueFull();
        }
    }

    private void checkReady() {
        synchronized (this) {
            if (ready || stream.writeQueueFull()) {
                return;
            }
            ready = true;
        }
        handleReady();
    }
}
