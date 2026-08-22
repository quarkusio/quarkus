package io.quarkus.deployment.dev;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
                new ByteArrayInputStream("quarkus-build-output/3  \n".getBytes(UTF_8))))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("token");
    }

    @Test
    void rejectsOversizedHello() {
        byte[] hello = ("quarkus-build-output/3 " + "x".repeat(BuildOutputChangesProtocol.MAX_HELLO_BYTES) + "\n")
                .getBytes(UTF_8);

        assertThatThrownBy(() -> BuildOutputChangesProtocol.readHello(new ByteArrayInputStream(hello)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum size");
    }

    @Test
    void rejectsTruncatedHello() {
        assertThatThrownBy(() -> BuildOutputChangesProtocol.readHello(
                new ByteArrayInputStream("quarkus-build-output/3 secret".getBytes(UTF_8))))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Truncated external build output hello");
    }

    @Test
    void roundTripsTypedMessages() throws Exception {
        BuildOutputChanges changes = changes("über/Foo.class");

        assertThat(BuildOutputChangesProtocol.decode(BuildOutputChangesProtocol.encodeChanges(Long.MAX_VALUE, changes)))
                .isEqualTo(new BuildOutputChangesProtocol.Changes(Long.MAX_VALUE, changes));
        assertThat(BuildOutputChangesProtocol.decode(
                BuildOutputChangesProtocol.encodeApplyResult(0, BuildOutputChangesApplyStatus.LIVE_RELOAD_DISABLED)))
                .isEqualTo(new BuildOutputChangesProtocol.ApplyResult(0,
                        BuildOutputChangesApplyStatus.LIVE_RELOAD_DISABLED));
        var state = new BuildOutputLiveReloadState(Long.MAX_VALUE, true);
        assertThat(BuildOutputChangesProtocol.decode(BuildOutputChangesProtocol.encodeLiveReloadState(state)))
                .isEqualTo(new BuildOutputChangesProtocol.LiveReloadState(state));
    }

    @Test
    void completeChangesSizeIncludesWorstCaseHeader() {
        BuildOutputChanges changes = changes("Foo.class");
        int jsonBytes = BuildOutputChangesJsonCodec.encode(changes).getBytes(UTF_8).length;

        assertThat(BuildOutputChangesProtocol.completeChangesPayloadBytes(changes))
                .isEqualTo(jsonBytes + ("CHANGES " + Long.MAX_VALUE + "\n").getBytes(UTF_8).length);
    }

    @Test
    void rejectsMalformedTypedMessagesWithoutEchoingValues() {
        for (String payload : List.of(
                "CHANGES -1\n{}",
                "CHANGES 9223372036854775808\n{}",
                "CHANGES 1",
                "CHANGES 1\n",
                "CHANGES 1 extra\n{}",
                "APPLY_RESULT 1 UNKNOWN",
                "APPLY_RESULT 1 APPLIED extra",
                "APPLY_RESULT 1 APPLIED\nbody",
                "LIVE_RELOAD_STATE -1 ENABLED",
                "LIVE_RELOAD_STATE 1 UNKNOWN",
                "UNKNOWN secret-value")) {
            assertThatThrownBy(() -> BuildOutputChangesProtocol.decode(payload))
                    .isInstanceOf(IOException.class)
                    .hasMessageNotContaining("secret-value");
        }
    }

    private static BuildOutputChanges changes(String path) {
        Path root = Path.of("build/classes");
        return new BuildOutputChanges(1, BuildOutputChangeStatus.BUILD_SUCCEEDED,
                List.of(new BuildOutputPathChange(root, root.resolve(path), BuildOutputChangeKind.MODIFIED)),
                null, null, null, null, null, false, false);
    }
}
