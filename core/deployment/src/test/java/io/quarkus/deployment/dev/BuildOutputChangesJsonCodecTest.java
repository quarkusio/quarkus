package io.quarkus.deployment.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.json.Json;

class BuildOutputChangesJsonCodecTest {

    @TempDir
    Path directory;

    @Test
    void roundTripsFullMessage() {
        var classesRoot = directory.resolve("classes");
        var resourcesRoot = directory.resolve("resources");
        var diagnostics = directory.resolve("diagnostics.txt");
        var changes = new BuildOutputChanges(42, BuildOutputChangeStatus.BUILD_SUCCEEDED,
                List.of(new BuildOutputPathChange(classesRoot, classesRoot.resolve("com/acme/Foo.class"),
                        BuildOutputChangeKind.MODIFIED)),
                List.of(new BuildOutputPathChange(resourcesRoot, resourcesRoot.resolve("application.properties"),
                        BuildOutputChangeKind.ADDED)),
                List.of(new BuildOutputPathChange(classesRoot, classesRoot.resolve("com/acme/FooTest.class"),
                        BuildOutputChangeKind.DELETED)),
                List.of(new BuildOutputPathChange(resourcesRoot, resourcesRoot.resolve("test.properties"),
                        BuildOutputChangeKind.MODIFIED)),
                "failed before", diagnostics, true, true);

        var encoded = BuildOutputChangesJsonCodec.encode(changes);
        var decoded = BuildOutputChangesJsonCodec.decode(encoded);

        assertThat(encoded).doesNotContain("token");
        assertThat(decoded.sequence()).isEqualTo(42);
        assertThat(decoded.status()).isEqualTo(BuildOutputChangeStatus.BUILD_SUCCEEDED);
        assertThat(decoded.mainClassChanges()).containsExactlyElementsOf(changes.mainClassChanges());
        assertThat(decoded.mainResourceChanges()).containsExactlyElementsOf(changes.mainResourceChanges());
        assertThat(decoded.testClassChanges()).containsExactlyElementsOf(changes.testClassChanges());
        assertThat(decoded.testResourceChanges()).containsExactlyElementsOf(changes.testResourceChanges());
        assertThat(decoded.failureSummary()).isEqualTo("failed before");
        assertThat(decoded.diagnosticsPath()).isEqualTo(diagnostics);
        assertThat(decoded.userInitiated()).isTrue();
        assertThat(decoded.forceRestart()).isTrue();
    }

    @Test
    void missingOptionalListsDefaultToEmpty() {
        String json = """
                {
                  "sequence": 1,
                  "status": "BUILD_FAILED"
                }
                """;

        var decoded = BuildOutputChangesJsonCodec.decode(json);

        assertThat(decoded.mainClassChanges()).isEmpty();
        assertThat(decoded.mainResourceChanges()).isEmpty();
        assertThat(decoded.testClassChanges()).isEmpty();
        assertThat(decoded.testResourceChanges()).isEmpty();
        assertThat(decoded.userInitiated()).isFalse();
        assertThat(decoded.forceRestart()).isFalse();
    }

    @Test
    void rejectsInvalidEnum() {
        String json = """
                {
                  "sequence": 1,
                  "status": "NOPE"
                }
                """;

        assertThatThrownBy(() -> BuildOutputChangesJsonCodec.decode(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");
    }

    @Test
    void rejectsChangedPathOutsideOutputRoot() {
        String json = toJson(Json.object()
                .put("sequence", 1)
                .put("status", "BUILD_SUCCEEDED")
                .put("mainClassChanges", Json.array()
                        .add(Json.object()
                                .put("outputRoot", directory.resolve("classes").toString())
                                .put("changedPath", directory.resolve("other/com/acme/Foo.class").toString())
                                .put("kind", "MODIFIED"))));

        assertThatThrownBy(() -> BuildOutputChangesJsonCodec.decode(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Changed path must be under output root");
    }

    private static String toJson(Json.JsonObjectBuilder object) {
        StringBuilder result = new StringBuilder();
        try {
            object.appendTo(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result.toString();
    }

    @Test
    void ignoresUnknownFields() {
        String json = """
                {
                  "sequence": 1,
                  "status": "BUILD_SUCCEEDED",
                  "unknown": "ignored"
                }
                """;

        assertThat(BuildOutputChangesJsonCodec.decode(json).sequence()).isEqualTo(1);
    }
}
