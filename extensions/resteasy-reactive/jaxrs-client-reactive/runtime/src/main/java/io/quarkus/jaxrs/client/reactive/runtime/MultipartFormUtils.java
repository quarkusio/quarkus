package io.quarkus.jaxrs.client.reactive.runtime;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.multipart.MultipartForm;

public class MultipartFormUtils {
    public static MultipartForm create() {
        return MultipartForm.create();
    }

    public static Buffer buffer(byte[] bytes) {
        return Buffer.buffer(bytes);
    }

    private MultipartFormUtils() {
    }
}
