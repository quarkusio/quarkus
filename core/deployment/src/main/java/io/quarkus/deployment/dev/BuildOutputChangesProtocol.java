package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

final class BuildOutputChangesProtocol {

    static final String VERSION = "quarkus-build-output/3";
    static final int MAX_HELLO_BYTES = 4096;

    private static final String CHANGES = "CHANGES";
    private static final String APPLY_RESULT = "APPLY_RESULT";
    private static final String LIVE_RELOAD_STATE = "LIVE_RELOAD_STATE";
    private static final int MAX_CHANGES_HEADER_BYTES = (CHANGES + " " + Long.MAX_VALUE + "\n")
            .getBytes(StandardCharsets.UTF_8).length;

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

    static String encodeChanges(long requestId, BuildOutputChanges changes) {
        requireNonNegative(requestId, "request ID");
        return CHANGES + " " + requestId + "\n" + BuildOutputChangesJsonCodec.encode(requireNonNull(changes, "changes"));
    }

    static String encodeApplyResult(long requestId, BuildOutputChangesApplyStatus status) {
        requireNonNegative(requestId, "request ID");
        return APPLY_RESULT + " " + requestId + " " + requireNonNull(status, "status").name();
    }

    static String encodeLiveReloadState(BuildOutputLiveReloadState state) {
        requireNonNull(state, "state");
        return LIVE_RELOAD_STATE + " " + state.generation() + " " + (state.enabled() ? "ENABLED" : "DISABLED");
    }

    static Message decode(String payload) throws IOException {
        requireNonNull(payload, "payload");
        int newline = payload.indexOf('\n');
        String header = newline < 0 ? payload : payload.substring(0, newline);
        String body = newline < 0 ? null : payload.substring(newline + 1);
        String[] tokens = header.split(" ", -1);
        if (tokens.length == 0) {
            throw invalidMessage();
        }
        return switch (tokens[0]) {
            case CHANGES -> decodeChanges(tokens, body);
            case APPLY_RESULT -> decodeApplyResult(tokens, body);
            case LIVE_RELOAD_STATE -> decodeLiveReloadState(tokens, body);
            default -> throw new IOException("Unknown external build output message kind");
        };
    }

    static int completeChangesPayloadBytes(BuildOutputChanges changes) {
        int bodyBytes = BuildOutputChangesJsonCodec.encode(requireNonNull(changes, "changes"))
                .getBytes(StandardCharsets.UTF_8).length;
        return Math.addExact(MAX_CHANGES_HEADER_BYTES, bodyBytes);
    }

    private static Changes decodeChanges(String[] tokens, String body) throws IOException {
        if (tokens.length != 2 || body == null || body.isEmpty()) {
            throw invalidMessage();
        }
        long requestId = parseNonNegative(tokens[1], "request ID");
        try {
            return new Changes(requestId, BuildOutputChangesJsonCodec.decode(body));
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid external build output changes body", e);
        }
    }

    private static ApplyResult decodeApplyResult(String[] tokens, String body) throws IOException {
        if (tokens.length != 3 || body != null) {
            throw invalidMessage();
        }
        long requestId = parseNonNegative(tokens[1], "request ID");
        try {
            return new ApplyResult(requestId, BuildOutputChangesApplyStatus.valueOf(tokens[2]));
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid external build output apply status", e);
        }
    }

    private static LiveReloadState decodeLiveReloadState(String[] tokens, String body) throws IOException {
        if (tokens.length != 3 || body != null) {
            throw invalidMessage();
        }
        long generation = parseNonNegative(tokens[1], "state generation");
        boolean enabled;
        if ("ENABLED".equals(tokens[2])) {
            enabled = true;
        } else if ("DISABLED".equals(tokens[2])) {
            enabled = false;
        } else {
            throw new IOException("Invalid external build output live-reload state");
        }
        return new LiveReloadState(new BuildOutputLiveReloadState(generation, enabled));
    }

    private static long parseNonNegative(String value, String name) throws IOException {
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0) {
                throw new IOException("External build output " + name + " must not be negative");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IOException("Invalid external build output " + name, e);
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException("External build output " + name + " must not be negative");
        }
    }

    private static IOException invalidMessage() {
        return new IOException("Invalid external build output message");
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

    sealed interface Message permits Changes, ApplyResult, LiveReloadState {
    }

    record Changes(long requestId, BuildOutputChanges changes) implements Message {
    }

    record ApplyResult(long requestId, BuildOutputChangesApplyStatus status) implements Message {
    }

    record LiveReloadState(BuildOutputLiveReloadState state) implements Message {
    }
}
