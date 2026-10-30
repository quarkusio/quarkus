package io.quarkus.deployment.pkg.steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.paths.PathTree;

class NativeImageConfigRootCollectorTest {

    private static final String CONFIG_DIR = "META-INF/native-image/org.acme/lib/";

    @TempDir
    Path dir;

    @Test
    void reflectAndJniConfigAreRootsInBothModes() {
        Path root = dir.resolve("lib");
        write(root, CONFIG_DIR + "reflect-config.json", "[{\"name\":\"org.acme.R\"}]");
        write(root, CONFIG_DIR + "jni-config.json", "[{\"name\":\"org.acme.J\"}]");

        assertThat(collect(false, root)).containsExactlyInAnyOrder("org.acme.R", "org.acme.J");
        assertThat(collect(true, root)).containsExactlyInAnyOrder("org.acme.R", "org.acme.J");
    }

    @Test
    void conditionalReflectEntryIsStillUnconditional() {
        Path root = dir.resolve("lib");
        write(root, CONFIG_DIR + "reflect-config.json",
                "[{\"condition\":{\"typeReachable\":\"org.acme.C\"},\"name\":\"org.acme.R\"}]");

        assertThat(collect(false, root)).containsExactly("org.acme.R");
    }

    @Test
    void nativeImagePropertiesAreIgnoredForJvm() {
        Path root = dir.resolve("lib");
        write(root, CONFIG_DIR + "native-image.properties",
                "Args = --features=org.acme.F --initialize-at-run-time=org.acme.Run");

        assertThat(collect(false, root)).isEmpty();
    }

    @Test
    void nativeImagePropertiesAreRootsForNative() {
        Path root = dir.resolve("lib");
        write(root, CONFIG_DIR + "native-image.properties", """
                # comment

                Args = --initialize-at-run-time=org.acme.Run,org.acme.pkg \\
                       --initialize-at-build-time=org.acme.Build \\
                       --features=org.acme.F1,org.acme.F2
                """);

        assertThat(collect(true, root)).containsExactlyInAnyOrder(
                "org.acme.Run", "org.acme.pkg", "org.acme.Build", "org.acme.F1", "org.acme.F2");
    }

    @Test
    void severalFlagsOnOneLineAreRootsForNative() {
        Path root = dir.resolve("lib");
        write(root, CONFIG_DIR + "native-image.properties",
                "Args = --features=org.acme.F --initialize-at-run-time=org.acme.Run,org.acme.Run2 --initialize-at-build-time=org.acme.Build");

        assertThat(collect(true, root)).containsExactlyInAnyOrder(
                "org.acme.F", "org.acme.Run", "org.acme.Run2", "org.acme.Build");
    }

    @Test
    void unreadableNativeImagePropertiesAreSkipped() {
        Path unreadable = dir.resolve("unreadable");
        write(unreadable, CONFIG_DIR + "native-image.properties", "Args = --features=org.acme.F");
        Path props = unreadable.resolve(CONFIG_DIR + "native-image.properties");
        assumeTrue(props.toFile().setReadable(false, false) && !Files.isReadable(props),
                "file permissions can't make the file unreadable");
        Path ok = dir.resolve("ok");
        write(ok, CONFIG_DIR + "reflect-config.json", "[{\"name\":\"org.acme.Ok\"}]");

        try {
            assertThat(collect(true, unreadable, ok)).containsExactly("org.acme.Ok");
        } finally {
            props.toFile().setReadable(true, false);
        }
    }

    @Test
    void windowsLineEndingsAreSupported() {
        Path root = dir.resolve("lib");
        write(root, CONFIG_DIR + "native-image.properties",
                "# comment\r\nArgs = --features=org.acme.F \\\r\n       --initialize-at-run-time=org.acme.Run\r\n");

        assertThat(collect(true, root)).containsExactlyInAnyOrder("org.acme.F", "org.acme.Run");
    }

    @Test
    void blankLastLineWithoutNewlineIsIgnored() {
        Path root = dir.resolve("lib");
        write(root, CONFIG_DIR + "native-image.properties", "Args = --features=org.acme.F\n   ");

        assertThat(collect(true, root)).containsExactly("org.acme.F");
    }

    @Test
    void onlyTopLevelEntryNamesAreRoots() {
        Path root = dir.resolve("lib");
        write(root, CONFIG_DIR + "reflect-config.json", """
                [
                  { "name": { "nested": "org.acme.NotAName" }, "fields": [ { "name": "field" } ] },
                  { "condition": { "typeReachable": "org.acme.C" }, "methods": [ { "name": "m", "parameterTypes": [] } ],
                    "name": "org.acme.R" },
                  { "name": "" },
                  { "name": 1 },
                  "org.acme.NotAnEntry"
                ]
                """);

        assertThat(collect(false, root)).containsExactly("org.acme.R");
    }

    @Test
    void invalidJsonIsSkipped() {
        Path broken = dir.resolve("broken");
        write(broken, CONFIG_DIR + "reflect-config.json", "{ not json");
        write(broken, CONFIG_DIR + "jni-config.json", "{\"name\":\"org.acme.Obj\"}");
        Path ok = dir.resolve("ok");
        write(ok, CONFIG_DIR + "reflect-config.json", "[{\"name\":\"org.acme.Ok\"}]");

        assertThat(collect(false, broken, ok)).containsExactly("org.acme.Ok");
    }

    @Test
    void configOutsideNativeImageDirIsIgnored() {
        Path root = dir.resolve("lib");
        write(root, "META-INF/other/reflect-config.json", "[{\"name\":\"org.acme.X\"}]");

        assertThat(collect(true, root)).isEmpty();
    }

    private static Set<String> collect(boolean nativeTarget, Path... roots) {
        final Set<String> collected = new HashSet<>();
        for (Path root : roots) {
            NativeImageConfigRootCollector.collect(PathTree.ofDirectoryOrArchive(root), nativeTarget, collected::add);
        }
        return collected;
    }

    private static void write(Path root, String relativePath, String content) {
        Path file = root.resolve(relativePath);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
