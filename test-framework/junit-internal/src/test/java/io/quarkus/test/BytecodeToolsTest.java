package io.quarkus.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gizmo2.Gizmo;

class BytecodeToolsTest {

    @Test
    void identicalBytecodeProducesEmptyDiff() {
        Map<String, byte[]> reference = generateSimpleClass("com.example.Hello");
        Map<String, byte[]> current = generateSimpleClass("com.example.Hello");
        var diff = BytecodeTools.diff(reference, current);
        assertThat(diff.isEmpty()).isTrue();
    }

    @Test
    void missingClassIsDetected() {
        Map<String, byte[]> reference = generateSimpleClass("com.example.Hello");
        Map<String, byte[]> current = new HashMap<>();

        var diff = BytecodeTools.diff(reference, current);

        assertThat(diff.isEmpty()).isFalse();
        assertThat(diff.missing()).containsExactly("com/example/Hello.class");
        assertThat(diff.extra()).isEmpty();
        assertThat(diff.changed()).isEmpty();
    }

    @Test
    void extraClassIsDetected() {
        Map<String, byte[]> reference = new HashMap<>();
        Map<String, byte[]> current = generateSimpleClass("com.example.Hello");

        var diff = BytecodeTools.diff(reference, current);

        assertThat(diff.isEmpty()).isFalse();
        assertThat(diff.missing()).isEmpty();
        assertThat(diff.extra()).containsExactly("com/example/Hello.class");
        assertThat(diff.changed()).isEmpty();
    }

    @Test
    void changedBytecodeIsDetected() {
        Map<String, byte[]> reference = generateClassWithField("com.example.Hello", "fieldA");
        Map<String, byte[]> current = generateClassWithField("com.example.Hello", "fieldB");

        var diff = BytecodeTools.diff(reference, current);

        assertThat(diff.isEmpty()).isFalse();
        assertThat(diff.missing()).isEmpty();
        assertThat(diff.extra()).isEmpty();
        assertThat(diff.changed()).containsExactly("com/example/Hello.class");
    }

    @Test
    void excludedClassIsNotReportedAsChanged() {
        String excluded = "io/quarkus/runner/recorded/WebJarProcessor$processWebJarDevMode";
        String resource = excluded + ".class";
        Map<String, byte[]> reference = Map.of(resource, new byte[] { 1, 2, 3 });
        Map<String, byte[]> current = Map.of(resource, new byte[] { 4, 5, 6 });

        var diff = BytecodeTools.diff(reference, current);

        assertThat(diff.isEmpty()).isTrue();
    }

    @Test
    void dumpCreatesExpectedFileStructure(@TempDir Path tempDir) throws Exception {
        Map<String, byte[]> reference = generateClassWithField("com.example.Foo", "fieldA");
        Map<String, byte[]> current = generateClassWithField("com.example.Foo", "fieldB");

        var diff = BytecodeTools.diff(reference, current);
        assertThat(diff.isEmpty()).isFalse();

        Path dumpDir = BytecodeTools.dumpReproducibilityMismatch(diff, reference, current, 2, tempDir);

        assertThat(dumpDir.resolve("run-1")).isDirectory();
        assertThat(dumpDir.resolve("run-2")).isDirectory();
        assertThat(dumpDir.resolve("mismatch.txt")).isRegularFile();
        assertThat(dumpDir.resolve("run-1/com/example/Foo.class")).isRegularFile();
        assertThat(dumpDir.resolve("run-2/com/example/Foo.class")).isRegularFile();
        assertThat(dumpDir.resolve("run-1-decompiled")).isDirectory();
        assertThat(dumpDir.resolve("run-2-decompiled")).isDirectory();

        String mismatchContent = Files.readString(dumpDir.resolve("mismatch.txt"));
        assertThat(mismatchContent).contains("CHANGED");
        assertThat(mismatchContent).contains("com/example/Foo.class");
    }

    private Map<String, byte[]> generateSimpleClass(String binaryName) {
        Map<String, byte[]> result = new HashMap<>();
        Gizmo g = Gizmo.create((path, bytes) -> result.put(path, bytes));
        g.class_(binaryName, cc -> {
            cc.public_();
        });
        return result;
    }

    private Map<String, byte[]> generateClassWithField(String binaryName, String fieldName) {
        Map<String, byte[]> result = new HashMap<>();
        Gizmo g = Gizmo.create((path, bytes) -> result.put(path, bytes));
        g.class_(binaryName, cc -> {
            cc.public_();
            cc.field(fieldName, fc -> {
                fc.setType(String.class);
                fc.private_();
            });
        });
        return result;
    }
}
