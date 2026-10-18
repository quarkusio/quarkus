package io.quarkus.test.common;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

class ProcessReader implements Runnable {

    private final InputStream inputStream;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final CompletableFuture<byte[]> result = new CompletableFuture<>();

    ProcessReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        byte[] b = new byte[100];
        int i;
        try {
            while ((i = inputStream.read(b)) > 0) {
                output.write(b, 0, i);
            }
            result.complete(output.toByteArray());
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
    }

    byte[] get() {
        try {
            return result.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}