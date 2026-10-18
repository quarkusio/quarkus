package io.quarkus.arc.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ReproducibilityCheckTest {

    @Test
    public void diffIdentical() {
        Map<String, byte[]> snapshot = Map.of(
                "com/example/Foo", new byte[] { 1, 2, 3 },
                "com/example/Bar", new byte[] { 4, 5, 6 });

        ReproducibilityCheck.Diff diff = ReproducibilityCheck.diff(snapshot, snapshot);
        assertTrue(diff.isEmpty());
        assertTrue(diff.missing().isEmpty());
        assertTrue(diff.extra().isEmpty());
        assertTrue(diff.changed().isEmpty());
    }

    @Test
    public void diffMissing() {
        Map<String, byte[]> reference = Map.of(
                "com/example/Foo", new byte[] { 1 },
                "com/example/Bar", new byte[] { 2 });
        Map<String, byte[]> current = Map.of(
                "com/example/Bar", new byte[] { 2 });

        ReproducibilityCheck.Diff diff = ReproducibilityCheck.diff(reference, current);
        assertFalse(diff.isEmpty());
        assertEquals(1, diff.missing().size());
        assertTrue(diff.missing().contains("com/example/Foo"));
        assertTrue(diff.extra().isEmpty());
        assertTrue(diff.changed().isEmpty());
    }

    @Test
    public void diffExtra() {
        Map<String, byte[]> reference = Map.of(
                "com/example/Foo", new byte[] { 1 });
        Map<String, byte[]> current = Map.of(
                "com/example/Foo", new byte[] { 1 },
                "com/example/New", new byte[] { 9 });

        ReproducibilityCheck.Diff diff = ReproducibilityCheck.diff(reference, current);
        assertFalse(diff.isEmpty());
        assertTrue(diff.missing().isEmpty());
        assertEquals(1, diff.extra().size());
        assertTrue(diff.extra().contains("com/example/New"));
        assertTrue(diff.changed().isEmpty());
    }

    @Test
    public void diffChanged() {
        Map<String, byte[]> reference = Map.of(
                "com/example/Foo", new byte[] { 1, 2 });
        Map<String, byte[]> current = Map.of(
                "com/example/Foo", new byte[] { 1, 3 });

        ReproducibilityCheck.Diff diff = ReproducibilityCheck.diff(reference, current);
        assertFalse(diff.isEmpty());
        assertTrue(diff.missing().isEmpty());
        assertTrue(diff.extra().isEmpty());
        assertEquals(1, diff.changed().size());
        assertTrue(diff.changed().contains("com/example/Foo"));
    }

    @Test
    public void diffCombined() {
        Map<String, byte[]> reference = Map.of(
                "com/example/Removed", new byte[] { 1 },
                "com/example/Same", new byte[] { 2 },
                "com/example/Modified", new byte[] { 3 });
        Map<String, byte[]> current = Map.of(
                "com/example/Same", new byte[] { 2 },
                "com/example/Modified", new byte[] { 99 },
                "com/example/Added", new byte[] { 4 });

        ReproducibilityCheck.Diff diff = ReproducibilityCheck.diff(reference, current);
        assertFalse(diff.isEmpty());
        assertEquals(1, diff.missing().size());
        assertEquals(1, diff.extra().size());
        assertEquals(1, diff.changed().size());
        assertTrue(diff.missing().contains("com/example/Removed"));
        assertTrue(diff.extra().contains("com/example/Added"));
        assertTrue(diff.changed().contains("com/example/Modified"));
    }

    @Test
    public void diffEmpty() {
        ReproducibilityCheck.Diff diff = ReproducibilityCheck.diff(Map.of(), Map.of());
        assertTrue(diff.isEmpty());
    }

    @Test
    public void dumpMismatchCreatesFiles(@TempDir Path tempDir) {
        byte[] refBytes = new byte[] { 1, 2, 3 };
        byte[] curBytes = new byte[] { 1, 2, 4 };
        Map<String, byte[]> reference = Map.of("com/example/Foo", refBytes);
        Map<String, byte[]> current = Map.of("com/example/Foo", curBytes);

        ReproducibilityCheck.Diff diff = ReproducibilityCheck.diff(reference, current);
        Path result = ReproducibilityCheck.dumpMismatch(diff, reference, current, 2, tempDir);

        assertEquals(tempDir, result);
        assertTrue(Files.exists(tempDir.resolve("run-1/com/example/Foo.class")));
        assertTrue(Files.exists(tempDir.resolve("run-2/com/example/Foo.class")));
        assertTrue(Files.exists(tempDir.resolve("mismatch.txt")));
    }

    @Test
    public void dumpMismatchMissingAndExtra(@TempDir Path tempDir) {
        Map<String, byte[]> reference = Map.of("com/example/Old", new byte[] { 1 });
        Map<String, byte[]> current = Map.of("com/example/New", new byte[] { 2 });

        ReproducibilityCheck.Diff diff = ReproducibilityCheck.diff(reference, current);
        ReproducibilityCheck.dumpMismatch(diff, reference, current, 3, tempDir);

        assertTrue(Files.exists(tempDir.resolve("run-1/com/example/Old.class")));
        assertFalse(Files.exists(tempDir.resolve("run-3/com/example/Old.class")));
        assertTrue(Files.exists(tempDir.resolve("run-3/com/example/New.class")));
        assertFalse(Files.exists(tempDir.resolve("run-1/com/example/New.class")));
    }

    @Test
    public void dumpMismatchIndexContainsSha256(@TempDir Path tempDir) throws Exception {
        Map<String, byte[]> reference = Map.of("com/example/Foo", new byte[] { 1 });
        Map<String, byte[]> current = Map.of("com/example/Foo", new byte[] { 2 });

        ReproducibilityCheck.Diff diff = ReproducibilityCheck.diff(reference, current);
        ReproducibilityCheck.dumpMismatch(diff, reference, current, 2, tempDir);

        String mismatch = Files.readString(tempDir.resolve("mismatch.txt"));
        assertTrue(mismatch.contains("CHANGED com/example/Foo"));
        assertTrue(mismatch.contains("run-1-sha256="));
        assertTrue(mismatch.contains("run-2-sha256="));
    }
}
