package io.quarkus.deployment.dev;

import static io.quarkus.deployment.dev.filesystem.watch.FileChangeEvent.Type.ADDED;
import static io.quarkus.deployment.dev.filesystem.watch.FileChangeEvent.Type.MODIFIED;
import static io.quarkus.deployment.dev.filesystem.watch.FileChangeEvent.Type.REMOVED;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.dev.filesystem.watch.FileChangeCallback;
import io.quarkus.deployment.dev.filesystem.watch.FileChangeEvent;
import io.quarkus.deployment.dev.filesystem.watch.WatchServiceFileSystemWatcher;

/**
 * Test file system watcher, non poll based
 */
public class FileSystemWatcherTestCase {
    public static final String DIR_NAME = "/fileSystemWatcherTest";
    public static final String EXISTING_FILE_NAME = "a.txt";
    public static final String EXISTING_DIR = "existingDir";

    private final BlockingDeque<Collection<FileChangeEvent>> results = new LinkedBlockingDeque<>();
    private final BlockingDeque<Collection<FileChangeEvent>> secondResults = new LinkedBlockingDeque<>();

    Path rootDir;
    Path existingSubDir;

    @BeforeEach
    public void setup() throws Exception {
        //this can be slow on other OS's
        //as it just relies on polling
        Assumptions.assumeTrue(RuntimeUpdatesProcessor.IS_LINUX);

        rootDir = Path.of(System.getProperty("java.io.tmpdir"), DIR_NAME);
        deleteRecursive(rootDir);

        Files.createDirectories(rootDir);
        Path existing = rootDir.resolve(EXISTING_FILE_NAME);
        touchFile(existing);
        existingSubDir = rootDir.resolve(EXISTING_DIR);
        Files.createDirectory(existingSubDir);
        existing = existingSubDir.resolve(EXISTING_FILE_NAME);
        touchFile(existing);
    }

    private static void touchFile(Path existing) throws IOException {
        Files.writeString(existing, "data" + System.currentTimeMillis());
    }

    @AfterEach
    public void after() throws IOException {
        if (rootDir != null) {
            deleteRecursive(rootDir);
        }
    }

    @Test
    public void testFileSystemWatcher() throws Exception {
        WatchServiceFileSystemWatcher watcher = new WatchServiceFileSystemWatcher("test", true);
        try {
            watcher.watchDirectoryRecursively(rootDir, new FileChangeCallback() {
                @Override
                public void handleChanges(Collection<FileChangeEvent> changes) {
                    results.add(changes);
                }
            });
            watcher.watchDirectoryRecursively(rootDir, new FileChangeCallback() {
                @Override
                public void handleChanges(Collection<FileChangeEvent> changes) {
                    secondResults.add(changes);
                }
            });
            //first add a file
            Path added = rootDir.resolve("newlyAddedFile.txt").toAbsolutePath();
            touchFile(added);
            checkResult(added, ADDED);
            Files.setLastModifiedTime(added, FileTime.fromMillis(500));
            checkResult(added, MODIFIED);
            Files.delete(added);
            Thread.sleep(1);
            checkResult(added, REMOVED);
            added = existingSubDir.resolve("newSubDirFile.txt");
            touchFile(added);
            checkResult(added, ADDED);
            Files.setLastModifiedTime(added, FileTime.fromMillis(500));
            checkResult(added, MODIFIED);
            Files.delete(added);
            Thread.sleep(1);
            checkResult(added, REMOVED);
            Path existing = rootDir.resolve(EXISTING_FILE_NAME);
            Files.delete(existing);
            Thread.sleep(1);
            checkResult(existing, REMOVED);
            Path newDir = rootDir.resolve("newlyCreatedDirectory");
            Files.createDirectory(newDir);
            checkResult(newDir, ADDED);
            added = newDir.resolve("newlyAddedFileInNewlyAddedDirectory.txt").toAbsolutePath();
            touchFile(added);
            checkResult(added, ADDED);
            Files.setLastModifiedTime(added, FileTime.fromMillis(500));
            checkResult(added, MODIFIED);
            Files.delete(added);
            Thread.sleep(1);
            checkResult(added, REMOVED);

        } finally {
            watcher.close();
        }

    }

    private void checkResult(Path file, FileChangeEvent.Type type) throws InterruptedException {
        Collection<FileChangeEvent> results = this.results.poll(20, TimeUnit.SECONDS);
        Collection<FileChangeEvent> secondResults = this.secondResults.poll(20, TimeUnit.SECONDS);
        Assertions.assertNotNull(results);
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(1, secondResults.size());
        FileChangeEvent res = results.iterator().next();
        FileChangeEvent res2 = secondResults.iterator().next();

        //sometime OS's will give a MODIFIED event before the REMOVED one
        //We consume these events here
        long endTime = System.currentTimeMillis() + 10000;
        while (type == REMOVED
                && (res.getType() == MODIFIED || res2.getType() == MODIFIED)
                && System.currentTimeMillis() < endTime) {
            FileChangeEvent[] nextEvents = consumeEvents();
            res = nextEvents[0];
            res2 = nextEvents[1];
        }

        //sometime OS's will give a MODIFIED event on its parent folder before the ADDED one
        //We consume these events here
        endTime = System.currentTimeMillis() + 10000;
        while (type == ADDED
                && (res.getType() == MODIFIED || res2.getType() == MODIFIED)
                && (res.getFile().equals(file.getParent()) || res2.getFile().equals(file.getParent()))
                && !Files.isDirectory(file)
                && System.currentTimeMillis() < endTime) {
            FileChangeEvent[] nextEvents = consumeEvents();
            res = nextEvents[0];
            res2 = nextEvents[1];
        }

        Assertions.assertEquals(file, res.getFile());
        Assertions.assertEquals(type, res.getType());
        Assertions.assertEquals(file, res2.getFile());
        Assertions.assertEquals(type, res2.getType());
    }

    private FileChangeEvent[] consumeEvents() throws InterruptedException {
        FileChangeEvent[] nextEvents = new FileChangeEvent[2];
        Collection<FileChangeEvent> results = this.results.poll(3, TimeUnit.SECONDS);
        Collection<FileChangeEvent> secondResults = this.secondResults.poll(3, TimeUnit.SECONDS);
        Assertions.assertNotNull(results);
        Assertions.assertNotNull(secondResults);
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(1, secondResults.size());
        nextEvents[0] = results.iterator().next();
        nextEvents[1] = secondResults.iterator().next();

        return nextEvents;
    }

    public static void deleteRecursive(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

}
