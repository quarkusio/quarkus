package io.quarkus.deployment.dev;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

final class BuildOutputChangesFrameCodec {

    static final int MAX_FRAME_BYTES = 1024 * 1024;

    private BuildOutputChangesFrameCodec() {
    }

    static void write(OutputStream outputStream, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_FRAME_BYTES) {
            throw new IOException("Build output changes frame exceeds maximum size");
        }
        DataOutputStream output = new DataOutputStream(outputStream);
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    static String read(InputStream inputStream) throws IOException {
        DataInputStream input = new DataInputStream(inputStream);
        int length = input.readInt();
        if (length < 0) {
            throw new IOException("Negative build output changes frame length");
        }
        if (length > MAX_FRAME_BYTES) {
            throw new IOException("Build output changes frame exceeds maximum size");
        }
        byte[] bytes = new byte[length];
        try {
            input.readFully(bytes);
        } catch (EOFException e) {
            throw new IOException("Truncated build output changes frame", e);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
