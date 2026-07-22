package io.quarkus.deployment.dev;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class BuildOutputChangesProtocolTest {

    @Test
    void writesAndReadsHello() throws Exception {
        var output = new ByteArrayOutputStream();

        BuildOutputChangesProtocol.writeHello(output, "secret");

        assertThat(BuildOutputChangesProtocol.readHello(new ByteArrayInputStream(output.toByteArray())))
                .isEqualTo("secret");
    }

    @Test
    void rejectsUnsupportedVersion() {
        assertThatThrownBy(() -> BuildOutputChangesProtocol.readHello(
                new ByteArrayInputStream("quarkus-build-output/2 secret\n".getBytes(UTF_8))))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unsupported external build output protocol version");
    }

    @Test
    void rejectsBlankToken() {
        assertThatThrownBy(() -> BuildOutputChangesProtocol.readHello(
                new ByteArrayInputStream("quarkus-build-output/1  \n".getBytes(UTF_8))))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("token");
    }

    @Test
    void rejectsOversizedHello() {
        byte[] hello = ("quarkus-build-output/1 " + "x".repeat(BuildOutputChangesProtocol.MAX_HELLO_BYTES) + "\n")
                .getBytes(UTF_8);

        assertThatThrownBy(() -> BuildOutputChangesProtocol.readHello(new ByteArrayInputStream(hello)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum size");
    }

    @Test
    void rejectsTruncatedHello() {
        assertThatThrownBy(() -> BuildOutputChangesProtocol.readHello(
                new ByteArrayInputStream("quarkus-build-output/1 secret".getBytes(UTF_8))))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Truncated external build output hello");
    }
}
