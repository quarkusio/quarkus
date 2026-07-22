package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

final class BuildOutputChangesProtocol {

    static final String VERSION = "quarkus-build-output/1";
    static final int MAX_HELLO_BYTES = 4096;

    private BuildOutputChangesProtocol() {
    }

    static void writeHello(OutputStream outputStream, String token) throws IOException {
        requireNonNull(outputStream, "outputStream");
        requireNonNull(token, "token");
        if (token.isBlank()) {
            throw new IOException("External build output hello token must not be blank");
        }
        if (token.indexOf('\n') >= 0 || token.indexOf('\r') >= 0) {
            throw new IOException("External build output hello token must not contain line breaks");
        }
        byte[] bytes = (VERSION + " " + token + "\n").getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_HELLO_BYTES) {
            throw new IOException("External build output hello exceeds maximum size");
        }
        outputStream.write(bytes);
        outputStream.flush();
    }

    static String readHello(InputStream inputStream) throws IOException {
        requireNonNull(inputStream, "inputStream");
        byte[] bytes = new byte[MAX_HELLO_BYTES];
        int offset = 0;
        while (offset < bytes.length) {
            int read = inputStream.read();
            if (read == -1) {
                if (offset == 0) {
                    throw new EOFException("External build output hello was not sent");
                }
                throw new IOException("Truncated external build output hello");
            }
            if (read == '\n') {
                String hello = new String(bytes, 0, offset, StandardCharsets.UTF_8);
                return token(hello);
            }
            bytes[offset++] = (byte) read;
        }
        throw new IOException("External build output hello exceeds maximum size");
    }

    private static String token(String hello) throws IOException {
        String prefix = VERSION + " ";
        if (!hello.startsWith(prefix)) {
            throw new IOException("Unsupported external build output protocol version");
        }
        String token = hello.substring(prefix.length());
        if (token.isBlank()) {
            throw new IOException("External build output hello token must not be blank");
        }
        return token;
    }
}
