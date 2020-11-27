package org.jboss.resteasy.reactive.server.spi;

import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public interface ServerHttpResponse {

    ServerHttpResponse setStatusCode(int code);

    ServerHttpResponse end();

    boolean headWritten();

    ServerHttpResponse end(byte[] data);

    ServerHttpResponse end(String data);

    ServerHttpResponse addResponseHeader(CharSequence name, CharSequence value);

    ServerHttpResponse setResponseHeader(CharSequence name, CharSequence value);

    ServerHttpResponse setResponseHeader(CharSequence name, Iterable<CharSequence> values);

    Iterable<Map.Entry<String, String>> getAllResponseHeaders();

    boolean closed();

    ServerHttpResponse setChunked(boolean chunked);

    ServerHttpResponse write(byte[] data, Consumer<Throwable> asyncResultHandler);

    CompletionStage<Void> write(byte[] data);

    OutputStream createResponseOutputStream();
}
