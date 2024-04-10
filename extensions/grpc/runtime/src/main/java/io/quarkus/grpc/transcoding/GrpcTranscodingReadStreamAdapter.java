package io.quarkus.grpc.transcoding;

import io.vertx.grpc.common.GrpcReadStream;

public class GrpcTranscodingReadStreamAdapter<T> {

    private GrpcReadStream<T> stream;
    private int request = 0;

    /**
     * Init the adapter with the stream.
     */
    public final void init(GrpcReadStream<T> stream, GrpcTranscodingMessageDecoder<T> decoder) {
        stream.messageHandler(msg -> {
            handleMessage(decoder.decode(msg));
        });
        stream.endHandler(v -> {
            handleClose();
        });
        this.stream = stream;
        stream.pause();
        if (request > 0) {
            stream.fetch(request);
        }
    }

    /**
     * Override this to handle close event
     */
    protected void handleClose() {

    }

    /**
     * Override this to handle message event
     */
    protected void handleMessage(T msg) {

    }

    /**
     * Request {@code num} messages
     */
    public final void request(int num) {
        if (stream != null) {
            stream.fetch(num);
        } else {
            request += num;
        }
    }
}
