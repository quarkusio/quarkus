package io.quarkus.amazon.lambda.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public interface LambdaOutputWriter {
    void writeValue(OutputStream os, Object obj) throws IOException;

    default void writeHeaders(HttpURLConnection conn) {
    }
}
