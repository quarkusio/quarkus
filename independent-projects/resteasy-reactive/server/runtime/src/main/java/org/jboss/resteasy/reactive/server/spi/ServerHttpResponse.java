package org.jboss.resteasy.reactive.server.spi;

import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public interface ServerHttpResponse {

    ServerHttpResponse setStatusCode(int code);

    int getStatusCode();

    ServerHttpResponse end();

    boolean headWritten();

    ServerHttpResponse end(byte[] data);

    ServerHttpResponse end(String data);

    ServerHttpResponse addResponseHeader(CharSequence name, CharSequence value);

    ServerHttpResponse setResponseHeader(CharSequence name, CharSequence value);

    ServerHttpResponse setResponseHeader(CharSequence name, Iterable<CharSequence> values);

    void clearResponseHeaders();

    Iterable<Map.Entry<String, String>> getAllResponseHeaders();

    boolean closed();

    ServerHttpResponse setChunked(boolean chunked);

    ServerHttpResponse write(byte[] data, Consumer<Throwable> asyncResultHandler);

    CompletionStage<Void> write(byte[] data);

    ServerHttpResponse sendFile(String path, long offset, long length);

    OutputStream createResponseOutputStream();

    void setPreCommitListener(Consumer<ResteasyReactiveRequestContext> task);

    ServerHttpResponse addCloseHandler(Runnable onClose);

    boolean isWriteQueueFull();

    ServerHttpResponse addDrainHandler(Runnable onDrain);
}
