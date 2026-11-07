package io.quarkus.vertx.http.runtime.devmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.dev.spi.RemoteDevState;

class RemoteDevFilterTest {

    private static final long MAX_BYTES = 10 * 1024 * 1024;

    @Test
    void remoteDevStateIsAllowed() throws Exception {
        IOException problem = new IOException("build failed", new IllegalStateException("cause"));
        problem.addSuppressed(new RuntimeException("suppressed"));
        RemoteDevState state = new RemoteDevState(new HashMap<>(Map.of("a.txt", "hash")), problem);

        Object result = read(serialize(state), MAX_BYTES);

        assertThat(result).isInstanceOfSatisfying(RemoteDevState.class, s -> {
            assertThat(s.getFileHashes()).containsEntry("a.txt", "hash");
            assertThat(s.getAugmentProblem()).hasMessage("build failed")
                    .hasCauseInstanceOf(IllegalStateException.class);
            assertThat(s.getAugmentProblem().getSuppressed()).hasSize(1);
        });
    }

    @Test
    void primitiveArraysAreAllowed() throws Exception {
        assertThat(read(serialize(new byte[] { 1, 2, 3 }), MAX_BYTES)).isEqualTo(new byte[] { 1, 2, 3 });
    }

    @Test
    void classesOutsideTheAllowlistAreRejected() {
        byte[] data = serialize(new NotAllowed());
        assertThatThrownBy(() -> read(data, MAX_BYTES)).isInstanceOf(InvalidClassException.class);

        byte[] arrayData = serialize(new NotAllowed[] { new NotAllowed() });
        assertThatThrownBy(() -> read(arrayData, MAX_BYTES)).isInstanceOf(InvalidClassException.class);
    }

    @Test
    void streamsOverTheByteLimitAreRejected() {
        // the filter is only called for classes and arrays, so something has to follow the large string
        byte[] data = serialize(new Object[] { "x".repeat(1000), Integer.valueOf(1) });
        assertThatThrownBy(() -> read(data, 100)).isInstanceOf(InvalidClassException.class);
    }

    private static Object read(byte[] data, long maxBytes) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            ois.setObjectInputFilter(RemoteSyncHandler.remoteDevFilter(maxBytes));
            return ois.readObject();
        }
    }

    private static byte[] serialize(Object value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    static class NotAllowed implements Serializable {
    }
}
