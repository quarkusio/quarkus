package io.quarkus.grpc;

import java.util.ArrayList;
import java.util.List;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Encodes and decodes exception cause chains in gRPC response trailers.
 */
public final class ExceptionCauseSupport {

    public static final Metadata.Key<String> EXCEPTION_CAUSES_KEY = Metadata.Key.of("quarkus-grpc-exception-causes",
            Metadata.ASCII_STRING_MARSHALLER);

    private static volatile boolean propagateExceptionCauses = true;

    private static final char ENTRY_SEPARATOR = '|';
    private static final char FIELD_SEPARATOR = ':';
    private static final int MAX_CAUSES = 16;
    private static final int MAX_MESSAGE_LENGTH = 1024;
    private static final int MAX_ENCODED_LENGTH = 4096;

    private ExceptionCauseSupport() {
    }

    public static void setPropagateExceptionCauses(boolean propagateExceptionCauses) {
        ExceptionCauseSupport.propagateExceptionCauses = propagateExceptionCauses;
    }

    public static boolean isPropagateExceptionCauses() {
        return propagateExceptionCauses;
    }

    /**
     * Encode the {@code getCause()} chain of the given throwable into the provided trailers.
     *
     * @param throwable the original server-side exception
     * @param trailers the trailers to populate
     */
    public static void encodeCauses(Throwable throwable, Metadata trailers) {
        if (throwable == null || trailers == null) {
            return;
        }
        String encoded = encodeCauseChain(throwable.getCause());
        if (!encoded.isEmpty()) {
            trailers.put(EXCEPTION_CAUSES_KEY, encoded);
        }
    }

    /**
     * Restore a {@link StatusRuntimeException} with a populated cause chain when trailers contain encoded causes.
     *
     * @param status the response status
     * @param trailers the response trailers
     * @param propagateExceptionCauses whether cause restoration is enabled
     * @return a status runtime exception, potentially with a restored cause chain
     */
    public static StatusRuntimeException toStatusRuntimeException(Status status, Metadata trailers,
            boolean propagateExceptionCauses) {
        StatusRuntimeException exception = status.asRuntimeException(trailers);
        attachCausesFromMetadata(exception, propagateExceptionCauses);
        return exception;
    }

    /**
     * Restore the exception cause chain from the {@link StatusRuntimeException} trailers when present.
     */
    public static void attachCausesFromMetadata(StatusRuntimeException failure, boolean propagateExceptionCauses) {
        restoreCauses(failure, propagateExceptionCauses);
    }

    /**
     * Restore the exception cause chain from the {@link StatusRuntimeException} trailers when present.
     */
    public static void attachCausesFromMetadata(StatusRuntimeException failure) {
        restoreCauses(failure, propagateExceptionCauses);
    }

    /**
     * Restore the exception cause chain from gRPC trailers when present.
     *
     * @return the original failure or a new {@link StatusRuntimeException} with a restored cause chain
     */
    public static Throwable restoreCauses(Throwable failure, boolean propagateExceptionCauses) {
        if (!propagateExceptionCauses || !(failure instanceof StatusRuntimeException statusRuntimeException)) {
            return failure;
        }
        Metadata trailers = statusRuntimeException.getTrailers();
        if (trailers == null || !trailers.containsKey(EXCEPTION_CAUSES_KEY)) {
            return failure;
        }
        Throwable decoded = decodeCauseChain(trailers.get(EXCEPTION_CAUSES_KEY));
        if (decoded == null) {
            return failure;
        }
        Status status = Status.fromCode(statusRuntimeException.getStatus().getCode())
                .withDescription(statusRuntimeException.getStatus().getDescription());
        final Throwable remoteCause = decoded;
        return new StatusRuntimeException(status, statusRuntimeException.getTrailers()) {
            @Override
            public synchronized Throwable getCause() {
                return remoteCause;
            }
        };
    }

    /**
     * Restore the exception cause chain from gRPC trailers when present.
     */
    public static Throwable restoreCauses(Throwable failure) {
        return restoreCauses(failure, propagateExceptionCauses);
    }

    static String encodeCauseChain(Throwable cause) {
        if (cause == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        Throwable current = cause;
        while (current != null && count < MAX_CAUSES) {
            if (count > 0) {
                builder.append(ENTRY_SEPARATOR);
            }
            appendEntry(builder, current.getClass().getName(), current.getMessage());
            current = current.getCause();
            count++;
        }
        String encoded = builder.toString();
        if (encoded.length() > MAX_ENCODED_LENGTH) {
            return encoded.substring(0, MAX_ENCODED_LENGTH);
        }
        return encoded;
    }

    static Throwable decodeCauseChain(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        List<GrpcRemoteException> causes = new ArrayList<>();
        for (String entry : splitEntries(encoded)) {
            GrpcRemoteException decoded = decodeEntry(entry);
            if (decoded != null) {
                causes.add(decoded);
            }
        }
        if (causes.isEmpty()) {
            return null;
        }
        Throwable root = causes.get(0);
        Throwable current = root;
        for (int i = 1; i < causes.size(); i++) {
            current.initCause(causes.get(i));
            current = causes.get(i);
        }
        return root;
    }

    private static List<String> splitEntries(String encoded) {
        List<String> entries = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < encoded.length(); i++) {
            char value = encoded.charAt(i);
            if (escaping) {
                current.append(value);
                escaping = false;
            } else if (value == '\\') {
                current.append(value);
                escaping = true;
            } else if (value == ENTRY_SEPARATOR) {
                entries.add(current.toString());
                current.setLength(0);
            } else {
                current.append(value);
            }
        }
        entries.add(current.toString());
        return entries;
    }

    private static void appendEntry(StringBuilder builder, String className, String message) {
        builder.append(escape(className == null ? "java.lang.Throwable" : className));
        builder.append(FIELD_SEPARATOR);
        builder.append(escape(message == null ? "" : truncate(message)));
    }

    private static GrpcRemoteException decodeEntry(String entry) {
        if (entry == null || entry.isEmpty()) {
            return null;
        }
        int separatorIndex = indexOfUnescapedSeparator(entry, FIELD_SEPARATOR);
        if (separatorIndex < 0) {
            return null;
        }
        String className = unescape(entry.substring(0, separatorIndex));
        String message = unescape(entry.substring(separatorIndex + 1));
        if (className.isEmpty()) {
            return null;
        }
        return new GrpcRemoteException(className, message);
    }

    private static int indexOfUnescapedSeparator(String value, char separator) {
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (escaping) {
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else if (current == separator) {
                return i;
            }
        }
        return -1;
    }

    private static String truncate(String message) {
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH);
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == ENTRY_SEPARATOR || current == FIELD_SEPARATOR || current == '\\') {
                builder.append('\\');
            }
            builder.append(current);
        }
        return builder.toString();
    }

    private static String unescape(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (escaping) {
                builder.append(current);
                escaping = false;
            } else if (current == '\\') {
                escaping = true;
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}
