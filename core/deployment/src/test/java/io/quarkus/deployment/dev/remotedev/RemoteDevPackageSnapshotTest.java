package io.quarkus.deployment.dev.remotedev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemoteDevPackageSnapshotTest {

    @TempDir
    Path directory;

    @Test
    void detectsAddedModifiedAndDeletedPackageFiles() throws Exception {
        Path root = Files.createDirectory(directory.resolve("quarkus-app"));
        write(root.resolve("app/app.jar"), "one");
        write(root.resolve("lib/main/lib.jar"), "lib");
        RemoteDevPackageSnapshot previous = RemoteDevPackageSnapshot.capture(root);

        write(root.resolve("app/app.jar"), "two");
        Files.delete(root.resolve("lib/main/lib.jar"));
        write(root.resolve("lib/deployment/appmodel.dat"), "model");
        RemoteDevPackageSnapshot current = RemoteDevPackageSnapshot.capture(root);

        RemoteDevPackageDiff diff = current.diffSince(previous, root);

        assertThat(diff.changed()).extracting(RemoteDevPackageChange::relativePath)
                .containsExactly("app/app.jar", "lib/deployment/appmodel.dat");
        assertThat(diff.deleted()).containsExactly("lib/main/lib.jar");
    }

    @Test
    void skipsQuarkusDirectoryAndProtectedDeletes() throws Exception {
        Path root = Files.createDirectory(directory.resolve("quarkus-app"));
        write(root.resolve("quarkus/quarkus-application.dat"), "ignored");
        write(root.resolve("root-file.txt"), "root");
        write(root.resolve("META-INF/MANIFEST.MF"), "manifest");
        write(root.resolve("META-INF/maven/app/pom.properties"), "maven");
        write(root.resolve("lib/main/removed.jar"), "remove");
        RemoteDevPackageSnapshot previous = RemoteDevPackageSnapshot.capture(root);

        Files.delete(root.resolve("quarkus/quarkus-application.dat"));
        Files.delete(root.resolve("root-file.txt"));
        Files.delete(root.resolve("META-INF/MANIFEST.MF"));
        Files.delete(root.resolve("META-INF/maven/app/pom.properties"));
        Files.delete(root.resolve("lib/main/removed.jar"));
        RemoteDevPackageSnapshot current = RemoteDevPackageSnapshot.capture(root);

        RemoteDevPackageDiff diff = current.diffSince(previous, root);

        assertThat(diff.changed()).isEmpty();
        assertThat(diff.deleted()).containsExactly("lib/main/removed.jar");
    }

    @Test
    void writesAndReadsStableSnapshot() throws Exception {
        Path root = Files.createDirectory(directory.resolve("quarkus-app"));
        write(root.resolve("app/app.jar"), "one");
        Path snapshotFile = directory.resolve("snapshot.tsv");

        RemoteDevPackageSnapshot.capture(root).write(snapshotFile);
        RemoteDevPackageSnapshot read = RemoteDevPackageSnapshot.read(snapshotFile);

        assertThat(Files.readString(snapshotFile)).contains("app/app.jar\t");
        assertThat(read.diffSince(RemoteDevPackageSnapshot.capture(root), root).isEmpty()).isTrue();
    }

    @Test
    void filtersDeletePathsThatAreNotPackageRelative() {
        RemoteDevPackageDiff diff = new RemoteDevPackageDiff(List.of(), List.of(
                "../outside.txt",
                "lib/../../outside.txt",
                "lib\\..\\..\\outside.txt",
                "/absolute/outside.txt",
                "\\\\server\\share\\outside.txt",
                "C:\\absolute\\outside.txt",
                "C:drive-relative.txt",
                "a:b/portable-package-path.txt",
                "lib/main/removed.jar"));

        assertThat(diff.deleted()).containsExactly("lib/main/removed.jar");
    }

    @Test
    void resolvesRequestedFilesStrictlyAndDeterministically() throws Exception {
        Path root = Files.createDirectory(directory.resolve("quarkus-app"));
        write(root.resolve("app/z.jar"), "z");
        write(root.resolve("app/a.jar"), "a");
        RemoteDevPackageSnapshot snapshot = RemoteDevPackageSnapshot.capture(root);

        RemoteDevPackageDiff requested = snapshot.requestedFiles(Set.of("app\\z.jar", "app/a.jar"), root);

        assertThat(requested.changed()).extracting(RemoteDevPackageChange::relativePath)
                .containsExactly("app/a.jar", "app/z.jar");
        assertThat(requested.changed()).extracting(RemoteDevPackageChange::file)
                .allMatch(path -> path.normalize().startsWith(root.normalize()));
    }

    @Test
    void rejectsMissingUnsafeAndDuplicateNormalizedRequestedFiles() throws Exception {
        Path root = Files.createDirectory(directory.resolve("quarkus-app"));
        write(root.resolve("app/application.jar"), "application");
        RemoteDevPackageSnapshot snapshot = RemoteDevPackageSnapshot.capture(root);

        assertThatThrownBy(() -> snapshot.requestedFiles(Set.of("app/missing.jar"), root))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("absent from the current snapshot");
        for (String unsafe : List.of(
                "../outside.txt",
                "lib/../../outside.txt",
                "/absolute/outside.txt",
                "\\\\server\\share\\outside.txt",
                "C:\\absolute\\outside.txt",
                "C:drive-relative.txt",
                "a:b/portable-package-path.txt",
                "./app/application.jar")) {
            assertThatThrownBy(() -> snapshot.requestedFiles(Set.of(unsafe), root))
                    .as("requested path %s", unsafe)
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("unsafe package path");
        }
        assertThatThrownBy(
                () -> snapshot.requestedFiles(Set.of("app/application.jar", "app\\application.jar"), root))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("duplicate normalized package path");
    }

    @Test
    void rejectsFileSymlinksWithoutReadingTheirTargets() throws Exception {
        Path root = Files.createDirectory(directory.resolve("quarkus-app"));
        Path outside = directory.resolve("outside-secret.txt");
        write(outside, "outside secret");
        Path link = root.resolve("app/secret.txt");
        Files.createDirectories(link.getParent());
        createSymbolicLinkOrAbort(link, outside);

        assertThatThrownBy(() -> RemoteDevPackageSnapshot.capture(root))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("unsupported entry type")
                .hasMessageContaining("app/secret.txt");
        assertThat(Files.readString(outside)).isEqualTo("outside secret");
    }

    @Test
    void rejectsDirectorySymlinksWithoutTraversingTheirTargets() throws Exception {
        Path root = Files.createDirectory(directory.resolve("quarkus-app"));
        Path outside = Files.createDirectory(directory.resolve("outside"));
        write(outside.resolve("secret.txt"), "outside secret");
        Path link = root.resolve("lib/external");
        Files.createDirectories(link.getParent());
        createSymbolicLinkOrAbort(link, outside);

        assertThatThrownBy(() -> RemoteDevPackageSnapshot.capture(root))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("unsupported entry type")
                .hasMessageContaining("lib/external");
    }

    @Test
    void rejectsUnsupportedPackageRoots() throws Exception {
        Path regularFile = directory.resolve("package-file");
        write(regularFile, "not a package directory");

        assertThatThrownBy(() -> RemoteDevPackageSnapshot.capture(regularFile))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("package root has an unsupported entry type")
                .hasMessageContaining("package-file");

        Path realRoot = Files.createDirectory(directory.resolve("real-root"));
        Path linkedRoot = directory.resolve("linked-root");
        createSymbolicLinkOrAbort(linkedRoot, realRoot);
        assertThatThrownBy(() -> RemoteDevPackageSnapshot.capture(linkedRoot))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("package root has an unsupported entry type")
                .hasMessageContaining("linked-root");
    }

    private static void createSymbolicLinkOrAbort(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException e) {
            Assumptions.abort("Symbolic links are not supported by this test environment: " + e);
        }
    }

    private static void write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
