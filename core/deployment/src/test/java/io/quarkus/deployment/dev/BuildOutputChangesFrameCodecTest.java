package io.quarkus.deployment.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class BuildOutputChangesFrameCodecTest {

    @Test
    void roundTripsPayload() throws Exception {
        var output = new ByteArrayOutputStream();

        BuildOutputChangesFrameCodec.write(output, "{\"ok\":true}");

        assertThat(BuildOutputChangesFrameCodec.read(new ByteArrayInputStream(output.toByteArray())))
                .isEqualTo("{\"ok\":true}");
    }

    @Test
    void rejectsNegativeLength() throws Exception {
        var output = new ByteArrayOutputStream();
        try (var data = new DataOutputStream(output)) {
            data.writeInt(-1);
        }

        assertThatThrownBy(() -> BuildOutputChangesFrameCodec.read(new ByteArrayInputStream(output.toByteArray())))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Negative");
    }

    @Test
    void rejectsOversizedLength() throws Exception {
        var output = new ByteArrayOutputStream();
        try (var data = new DataOutputStream(output)) {
            data.writeInt(BuildOutputChangesFrameCodec.MAX_FRAME_BYTES + 1);
        }

        assertThatThrownBy(() -> BuildOutputChangesFrameCodec.read(new ByteArrayInputStream(output.toByteArray())))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum");
    }

    @Test
    void rejectsTruncatedPayload() throws Exception {
        var output = new ByteArrayOutputStream();
        try (var data = new DataOutputStream(output)) {
            data.writeInt(10);
            data.write("short".getBytes(StandardCharsets.UTF_8));
        }

        assertThatThrownBy(() -> BuildOutputChangesFrameCodec.read(new ByteArrayInputStream(output.toByteArray())))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Truncated");
    }
}
