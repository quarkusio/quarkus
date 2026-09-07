package io.quarkus.deployment.dev.remotedev;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

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

    private static void write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
