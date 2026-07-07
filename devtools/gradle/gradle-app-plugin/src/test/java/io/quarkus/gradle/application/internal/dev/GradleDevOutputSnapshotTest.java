package io.quarkus.gradle.application.internal.dev;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.deployment.dev.BuildOutputChangeKind;

class GradleDevOutputSnapshotTest {

    @TempDir
    Path testDirectory;

    @Test
    void detectsPreciseChangesBetweenWholeTreeSnapshots() throws Exception {
        Path classes = Files.createDirectories(testDirectory.resolve("classes"));
        Path resources = Files.createDirectories(testDirectory.resolve("resources"));
        Path runtime = Files.createDirectories(testDirectory.resolve("runtime"));
        Path unchanged = Files.writeString(classes.resolve("Unchanged.class"), "same");
        Path modified = Files.writeString(classes.resolve("Modified.class"), "old");
        Path removed = Files.writeString(resources.resolve("removed.txt"), "old");
        Path ignored = Files.writeString(classes.resolve("Ignored.java"), "old");
        Path runtimeJar = Files.writeString(runtime.resolve("dependency.jar"), "old");
        for (Path path : List.of(unchanged, modified, removed, ignored, runtimeJar)) {
            Files.setLastModifiedTime(path, FileTime.fromMillis(1));
        }

        GradleDevOutputSnapshot previous = snapshot(classes, resources, runtimeJar);
        Files.writeString(modified, "new");
        Files.setLastModifiedTime(modified, FileTime.fromMillis(2));
        Files.delete(removed);
        Files.writeString(ignored, "new");
        Path added = Files.writeString(resources.resolve("added.txt"), "new");
        Files.writeString(runtimeJar, "new");
        Files.setLastModifiedTime(runtimeJar, FileTime.fromMillis(2));

        GradleDevOutputSnapshot current = snapshot(classes, resources, runtimeJar);

        assertThat(current.changesSince(previous))
                .extracting(change -> change.scope() + ":" + change.kind() + ":"
                        + change.outputRoot().relativize(change.changedPath()))
                .containsExactlyInAnyOrder(
                        GradleDevOutputScope.MAIN_CLASSES + ":" + BuildOutputChangeKind.MODIFIED + ":Modified.class",
                        GradleDevOutputScope.MAIN_RESOURCES + ":" + BuildOutputChangeKind.ADDED + ":added.txt",
                        GradleDevOutputScope.MAIN_RESOURCES + ":" + BuildOutputChangeKind.DELETED + ":removed.txt",
                        GradleDevOutputScope.RUNTIME_JARS + ":" + BuildOutputChangeKind.MODIFIED + ":");
        assertThat(current.runtimeJarChangesSince(previous)).isEqualTo(1);
        assertThat(current.changesSince(previous))
                .extracting(GradleDevFileChange::changedPath)
                .doesNotContain(unchanged, ignored)
                .contains(added);
    }

    @Test
    void roundTripsSnapshotFile() throws Exception {
        Path classes = Files.createDirectories(testDirectory.resolve("classes"));
        Files.writeString(classes.resolve("Application.class"), "class");
        Path snapshotFile = testDirectory.resolve("snapshot.tsv");

        GradleDevOutputSnapshot snapshot = snapshot(classes);
        snapshot.write(snapshotFile);

        assertThat(GradleDevOutputSnapshot.read(snapshotFile).changesSince(snapshot)).isEmpty();
    }

    @Test
    void ignoresTimestampOnlyChanges() throws Exception {
        Path resources = Files.createDirectories(testDirectory.resolve("resources"));
        Path resource = Files.writeString(resources.resolve("application.properties"), "same");
        Files.setLastModifiedTime(resource, FileTime.fromMillis(1));
        GradleDevOutputSnapshot previous = snapshot(resources);

        Files.setLastModifiedTime(resource, FileTime.fromMillis(2));
        GradleDevOutputSnapshot current = snapshot(resources);

        assertThat(current.changesSince(previous)).isEmpty();
    }

    @Test
    void incrementallyUpdatesChangedFilesWithoutRecapturingUnchangedRoots() throws Exception {
        Path classes = Files.createDirectories(testDirectory.resolve("classes"));
        Path resources = Files.createDirectories(testDirectory.resolve("resources"));
        Path unchanged = Files.writeString(classes.resolve("Unchanged.class"), "same");
        Path modified = Files.writeString(classes.resolve("Modified.class"), "old");
        Path removed = Files.writeString(resources.resolve("removed.txt"), "old");
        GradleDevOutputSnapshot previous = snapshot(classes, resources);

        Files.writeString(modified, "new");
        Files.delete(removed);
        Path added = Files.writeString(resources.resolve("added.txt"), "new");
        Files.writeString(unchanged, "changed-but-not-reported");

        GradleDevOutputSnapshot updated = previous.updatedBy(List.of(
                change(GradleDevOutputScope.MAIN_CLASSES, classes, modified, BuildOutputChangeKind.MODIFIED),
                change(GradleDevOutputScope.MAIN_RESOURCES, resources, removed, BuildOutputChangeKind.DELETED),
                change(GradleDevOutputScope.MAIN_RESOURCES, resources, added, BuildOutputChangeKind.ADDED)));

        assertThat(updated.changesSince(previous))
                .extracting(change -> change.scope() + ":" + change.kind() + ":"
                        + change.outputRoot().relativize(change.changedPath()))
                .containsExactlyInAnyOrder(
                        GradleDevOutputScope.MAIN_CLASSES + ":" + BuildOutputChangeKind.MODIFIED + ":Modified.class",
                        GradleDevOutputScope.MAIN_RESOURCES + ":" + BuildOutputChangeKind.DELETED + ":removed.txt",
                        GradleDevOutputScope.MAIN_RESOURCES + ":" + BuildOutputChangeKind.ADDED + ":added.txt");
        assertThat(updated.changesSince(previous))
                .extracting(GradleDevFileChange::changedPath)
                .doesNotContain(unchanged);
    }

    @Test
    void incrementalDeleteOfDirectoryRemovesTrackedChildren() throws Exception {
        Path resources = Files.createDirectories(testDirectory.resolve("resources"));
        Path directory = Files.createDirectories(resources.resolve("nested"));
        Path child = Files.writeString(directory.resolve("application.properties"), "old");
        GradleDevOutputSnapshot previous = snapshot(resources);

        Files.delete(child);
        Files.delete(directory);

        GradleDevOutputSnapshot updated = previous.updatedBy(List.of(
                change(GradleDevOutputScope.MAIN_RESOURCES, resources, directory, BuildOutputChangeKind.DELETED)));

        assertThat(updated.changesSince(previous))
                .extracting(change -> Map.entry(change.kind(), change.outputRoot().relativize(change.changedPath())))
                .containsExactly(Map.entry(BuildOutputChangeKind.DELETED, Path.of("nested", "application.properties")));
    }

    @Test
    void incrementalUpdateIgnoresUntrackedClassSourceFiles() throws Exception {
        Path classes = Files.createDirectories(testDirectory.resolve("classes"));
        GradleDevOutputSnapshot previous = snapshot(classes);
        Path ignored = Files.writeString(classes.resolve("Ignored.java"), "new");

        GradleDevOutputSnapshot updated = previous.updatedBy(List.of(
                change(GradleDevOutputScope.MAIN_CLASSES, classes, ignored, BuildOutputChangeKind.ADDED)));

        assertThat(updated.changesSince(previous)).isEmpty();
    }

    @Test
    void ignoresObsoleteMtimeBasedSnapshotFiles() throws Exception {
        Path snapshotFile = testDirectory.resolve("snapshot.tsv");
        Files.writeString(snapshotFile, "MAIN_RESOURCES\tL3Jvb3Q\tL3Jvb3QvYXBwbGljYXRpb24ucHJvcGVydGllcw\t42\t4\n");

        assertThat(GradleDevOutputSnapshot.read(snapshotFile).isEmpty()).isTrue();
    }

    private static GradleDevOutputSnapshot snapshot(Path... roots) throws Exception {
        return GradleDevOutputSnapshot.capture(List.of(roots).stream()
                .map(root -> new GradleDevOutputSnapshot.Root(scope(root), root))
                .toList());
    }

    private static GradleDevOutputScope scope(Path root) {
        String name = root.getFileName().toString();
        if (name.equals("classes")) {
            return GradleDevOutputScope.MAIN_CLASSES;
        }
        if (name.equals("resources")) {
            return GradleDevOutputScope.MAIN_RESOURCES;
        }
        return GradleDevOutputScope.RUNTIME_JARS;
    }

    private static GradleDevFileChange change(GradleDevOutputScope scope, Path outputRoot, Path changedPath,
            BuildOutputChangeKind kind) {
        return new GradleDevFileChange(scope, outputRoot, changedPath, kind);
    }
}
