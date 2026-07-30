/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.docs.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CopyExampleSourceTest {

    @TempDir
    Path tempDir;

    @Test
    void copiesSourceAndReplacesSourceComments() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path manifests = Files.createDirectory(tempDir.resolve("manifests"));
        Path output = tempDir.resolve("output");
        write(root.resolve("examples/Example.java"),
                "// Source: {{source}}\n# Source: {{source}}\n// Source: unchanged\nclass Example {}\n");
        writeManifest(manifests.resolve("copy-examples.yaml"),
                "examples/Example.java", "copied/Example.java");

        copier(output, root, manifests).run();

        assertEquals(List.of(
                "// Source: examples/Example.java",
                "# Source: examples/Example.java",
                "// Source: unchanged",
                "class Example {}"), Files.readAllLines(output.resolve("copied/Example.java")));
    }

    @Test
    void reportsMissingSourceWithManifestAndTarget() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path manifests = Files.createDirectory(tempDir.resolve("manifests"));
        Path manifest = manifests.resolve("missing-examples.yaml");
        writeManifest(manifest, "examples/Missing.java", "copied/Missing.java");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> copier(tempDir.resolve("output"), root, manifests).run());

        assertContains(failure.getMessage(),
                "Example source copy failed:",
                "Missing sources:",
                "manifest: " + manifest,
                "target: copied/Missing.java",
                "source: examples/Missing.java",
                root.resolve("examples/Missing.java").toString());
    }

    @Test
    void reportsDuplicateTargetWithinManifestAndKeepsFirstSource() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path manifests = Files.createDirectory(tempDir.resolve("manifests"));
        Path output = tempDir.resolve("output");
        Path manifest = manifests.resolve("duplicate-examples.yaml");
        write(root.resolve("first.txt"), "first\n");
        write(root.resolve("second.txt"), "second\n");
        write(manifest, """
                examples:
                - source: first.txt
                  target: shared.txt
                - source: second.txt
                  target: shared.txt
                """);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> copier(output, root, manifests).run());

        assertContains(failure.getMessage(),
                "Duplicate targets:",
                "target: shared.txt",
                "manifest: " + manifest,
                "source: first.txt",
                "source: second.txt",
                root.resolve("first.txt").toString(),
                root.resolve("second.txt").toString());
        assertEquals("first\n", Files.readString(output.resolve("shared.txt")));
    }

    @Test
    void normalizesTargetsBeforeDetectingAliases() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path manifests = Files.createDirectory(tempDir.resolve("manifests"));
        Path output = tempDir.resolve("output");
        Path manifest = manifests.resolve("duplicate-examples.yaml");
        write(root.resolve("first.txt"), "first\n");
        write(root.resolve("second.txt"), "second\n");
        write(manifest, """
                examples:
                - source: first.txt
                  target: nested/../shared.txt
                - source: second.txt
                  target: shared.txt
                """);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> copier(output, root, manifests).run());

        assertContains(failure.getMessage(), "Duplicate targets:", "target: shared.txt");
        assertEquals("first\n", Files.readString(output.resolve("shared.txt")));
    }

    @Test
    void rejectsAbsoluteAndEscapingTargetsWithoutWritingOutsideOutput() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path manifests = Files.createDirectory(tempDir.resolve("manifests"));
        Path output = tempDir.resolve("output");
        Path escaped = tempDir.resolve("escaped.txt");
        Path absolute = tempDir.resolve("absolute.txt").toAbsolutePath();
        write(root.resolve("source.txt"), "content\n");
        write(manifests.resolve("invalid-examples.yaml"), """
                examples:
                - source: source.txt
                  target: ../escaped.txt
                - source: source.txt
                  target: %s
                """.formatted(absolute));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> copier(output, root, manifests).run());

        assertContains(failure.getMessage(),
                "Invalid targets outside the output directory:",
                "target: ../escaped.txt",
                "target: " + absolute);
        assertFalse(Files.exists(escaped));
        assertFalse(Files.exists(absolute));
    }

    @Test
    void rejectsAbsoluteAndEscapingSourcesOutsideProjectRoot() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path manifests = Files.createDirectory(tempDir.resolve("manifests"));
        Path output = tempDir.resolve("output");
        Path escaped = tempDir.resolve("escaped.txt");
        Path absolute = tempDir.resolve("absolute.txt").toAbsolutePath();
        write(escaped, "escaped\n");
        write(absolute, "absolute\n");
        write(manifests.resolve("invalid-examples.yaml"), """
                examples:
                - source: ../escaped.txt
                  target: escaped.txt
                - source: %s
                  target: absolute.txt
                """.formatted(absolute));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> copier(output, root, manifests).run());

        assertContains(failure.getMessage(),
                "Invalid sources outside the project root:",
                "source: ../escaped.txt",
                "source: " + absolute);
        assertFalse(Files.exists(output.resolve("escaped.txt")));
        assertFalse(Files.exists(output.resolve("absolute.txt")));
    }

    @Test
    void rejectsCaseOnlyTargetAliasesPortably() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path manifests = Files.createDirectory(tempDir.resolve("manifests"));
        Path output = tempDir.resolve("output");
        write(root.resolve("first.txt"), "first\n");
        write(root.resolve("second.txt"), "second\n");
        write(manifests.resolve("duplicate-examples.yaml"), """
                examples:
                - source: first.txt
                  target: Shared.txt
                - source: second.txt
                  target: shared.txt
                """);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> copier(output, root, manifests).run());

        assertContains(failure.getMessage(), "Duplicate targets:", "target: shared.txt");
        assertEquals("first\n", Files.readString(output.resolve("Shared.txt")));
        try (var entries = Files.list(output)) {
            assertEquals(List.of(output.resolve("Shared.txt")), entries.toList());
        }
    }

    @Test
    void missingFirstDuplicateRetainsTargetOwnership() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path manifests = Files.createDirectory(tempDir.resolve("manifests"));
        Path output = tempDir.resolve("output");
        Path manifest = manifests.resolve("duplicate-examples.yaml");
        write(root.resolve("second.txt"), "second\n");
        write(manifest, """
                examples:
                - source: missing.txt
                  target: shared.txt
                - source: second.txt
                  target: shared.txt
                """);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> copier(output, root, manifests).run());

        assertContains(failure.getMessage(),
                "Missing sources:",
                "source: missing.txt",
                "Duplicate targets:",
                "source: second.txt");
        assertFalse(Files.exists(output.resolve("shared.txt")));
    }

    @Test
    void sortsManifestsAcrossSourceRootsAndKeepsFirstTarget() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path firstManifests = Files.createDirectory(tempDir.resolve("a-manifests"));
        Path laterManifests = Files.createDirectory(tempDir.resolve("z-manifests"));
        Path output = tempDir.resolve("output");
        write(root.resolve("first.txt"), "first\n");
        write(root.resolve("later.txt"), "later\n");
        Path firstManifest = firstManifests.resolve("first-examples.yaml");
        Path laterManifest = laterManifests.resolve("later-examples.yaml");
        writeManifest(firstManifest, "first.txt", "shared.txt");
        writeManifest(laterManifest, "later.txt", "shared.txt");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> copier(output, root, laterManifests, firstManifests).run());

        assertEquals("first\n", Files.readString(output.resolve("shared.txt")));
        String message = failure.getMessage();
        assertContains(message,
                "manifest: " + firstManifest,
                "manifest: " + laterManifest,
                root.resolve("first.txt").toString(),
                root.resolve("later.txt").toString());
        assertTrue(message.indexOf("manifest: " + firstManifest) < message.indexOf("manifest: " + laterManifest),
                message);
    }

    @Test
    void aggregatesAndSortsMissingAndDuplicateFailures() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path manifests = Files.createDirectory(tempDir.resolve("manifests"));
        write(root.resolve("a.txt"), "a\n");
        write(root.resolve("z.txt"), "z\n");
        Path firstManifest = manifests.resolve("a-examples.yaml");
        Path laterManifest = manifests.resolve("z-examples.yaml");
        write(firstManifest, """
                examples:
                - source: z-missing.txt
                  target: z-missing.txt
                - source: a.txt
                  target: z-shared.txt
                - source: a.txt
                  target: a-shared.txt
                """);
        write(laterManifest, """
                examples:
                - source: a-missing.txt
                  target: a-missing.txt
                - source: z.txt
                  target: z-shared.txt
                - source: z.txt
                  target: a-shared.txt
                """);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> copier(tempDir.resolve("output"), root, manifests).run());

        String message = failure.getMessage();
        assertContains(message,
                "Missing sources:",
                "Duplicate targets:",
                "source: a-missing.txt",
                "source: z-missing.txt",
                "source: a.txt",
                "source: z.txt");
        assertTrue(message.indexOf("source: z-missing.txt") < message.indexOf("source: a-missing.txt"), message);
        assertTrue(message.indexOf("Missing sources:") < message.indexOf("Duplicate targets:"), message);
        assertTrue(message.indexOf("- target: a-shared.txt") < message.indexOf("- target: z-shared.txt"), message);
    }

    @Test
    void resetsRunLocalStateBetweenInvocations() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("root"));
        Path manifests = Files.createDirectory(tempDir.resolve("manifests"));
        Path source = root.resolve("source.txt");
        Path output = tempDir.resolve("output");
        write(source, "first\n");
        writeManifest(manifests.resolve("repeat-examples.yaml"), "source.txt", "target.txt");
        CopyExampleSource copier = copier(output, root, manifests);

        copier.run();
        write(source, "second\n");
        copier.run();

        assertEquals("second\n", Files.readString(output.resolve("target.txt")));
    }

    private static CopyExampleSource copier(Path output, Path root, Path... manifests) {
        CopyExampleSource copier = new CopyExampleSource();
        copier.outputPath = output;
        copier.rootPath = root;
        copier.srcPaths = List.of(manifests);
        return copier;
    }

    private static void writeManifest(Path path, String source, String target) throws Exception {
        write(path, """
                examples:
                - source: %s
                  target: %s
                """.formatted(source, target));
    }

    private static void write(Path path, String content) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private static void assertContains(String actual, String... expectedParts) {
        for (String expectedPart : expectedParts) {
            assertTrue(actual.contains(expectedPart),
                    () -> "Expected <%s> to contain <%s>".formatted(actual, expectedPart));
        }
    }
}
