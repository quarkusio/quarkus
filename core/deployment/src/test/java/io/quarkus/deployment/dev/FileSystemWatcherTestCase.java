package io.quarkus.deployment.dev;

import static io.quarkus.deployment.dev.filewatch.FileChangeEvent.Type.ADDED;
import static io.quarkus.deployment.dev.filewatch.FileChangeEvent.Type.MODIFIED;
import static io.quarkus.deployment.dev.filewatch.FileChangeEvent.Type.REMOVED;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.deployment.dev.filewatch.FileChangeCallback;
import io.quarkus.deployment.dev.filewatch.FileChangeEvent;
import io.quarkus.deployment.dev.filewatch.WatchServiceFileSystemWatcher;

/**
 * Test file system watcher, non poll based
 */
public class FileSystemWatcherTestCase {
    public static final String DIR_NAME = "/fileSystemWatcherTest";
    public static final String EXISTING_FILE_NAME = "a.txt";
    public static final String EXISTING_DIR = "existingDir";

    private final BlockingDeque<Collection<FileChangeEvent>> results = new LinkedBlockingDeque<>();
    private final BlockingDeque<Collection<FileChangeEvent>> secondResults = new LinkedBlockingDeque<>();

    File rootDir;
    File existingSubDir;

    @BeforeEach
    public void setup() throws Exception {
        //this can be slow on other OS's
        //as it just relies on polling
        Assumptions.assumeTrue(RuntimeUpdatesProcessor.IS_LINUX);

        rootDir = new File(System.getProperty("java.io.tmpdir") + DIR_NAME);
        deleteRecursive(rootDir);

        rootDir.mkdirs();
        File existing = new File(rootDir, EXISTING_FILE_NAME);
        touchFile(existing);
        existingSubDir = new File(rootDir, EXISTING_DIR);
        existingSubDir.mkdir();
        existing = new File(existingSubDir, EXISTING_FILE_NAME);
        touchFile(existing);
    }

    private static void touchFile(File existing) throws IOException {
        FileOutputStream out = new FileOutputStream(existing);
        try {
            out.write(("data" + System.currentTimeMillis()).getBytes());
            out.flush();
        } finally {
            out.close();
        }
    }

    @AfterEach
    public void after() {
        if (rootDir != null) {
            deleteRecursive(rootDir);
        }
    }

    @Test
    public void testFileSystemWatcher() throws Exception {
        WatchServiceFileSystemWatcher watcher = new WatchServiceFileSystemWatcher("test", true);
        try {
            watcher.watchPath(rootDir, new FileChangeCallback() {
                @Override
                public void handleChanges(Collection<FileChangeEvent> changes) {
                    results.add(changes);
                }
            });
            watcher.watchPath(rootDir, new FileChangeCallback() {
                @Override
                public void handleChanges(Collection<FileChangeEvent> changes) {
                    secondResults.add(changes);
                }
            });
            //first add a file
            File added = new File(rootDir, "newlyAddedFile.txt").getAbsoluteFile();
            touchFile(added);
            checkResult(added, ADDED);
            added.setLastModified(500);
            checkResult(added, MODIFIED);
            added.delete();
            Thread.sleep(1);
            checkResult(added, REMOVED);
            added = new File(existingSubDir, "newSubDirFile.txt");
            touchFile(added);
            checkResult(added, ADDED);
            added.setLastModified(500);
            checkResult(added, MODIFIED);
            added.delete();
            Thread.sleep(1);
            checkResult(added, REMOVED);
            File existing = new File(rootDir, EXISTING_FILE_NAME);
            existing.delete();
            Thread.sleep(1);
            checkResult(existing, REMOVED);
            File newDir = new File(rootDir, "newlyCreatedDirectory");
            newDir.mkdir();
            checkResult(newDir, ADDED);
            added = new File(newDir, "newlyAddedFileInNewlyAddedDirectory.txt").getAbsoluteFile();
            touchFile(added);
            checkResult(added, ADDED);
            added.setLastModified(500);
            checkResult(added, MODIFIED);
            added.delete();
            Thread.sleep(1);
            checkResult(added, REMOVED);

        } finally {
            watcher.close();
        }

    }

    private void checkResult(File file, FileChangeEvent.Type type) throws InterruptedException {
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
                && (res.getFile().equals(file.getParentFile()) || res2.getFile().equals(file.getParentFile()))
                && !file.isDirectory()
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
        Collection<FileChangeEvent> results = this.results.poll(1, TimeUnit.SECONDS);
        Collection<FileChangeEvent> secondResults = this.secondResults.poll(1, TimeUnit.SECONDS);
        Assertions.assertNotNull(results);
        Assertions.assertNotNull(secondResults);
        Assertions.assertEquals(1, results.size());
        Assertions.assertEquals(1, secondResults.size());
        nextEvents[0] = results.iterator().next();
        nextEvents[1] = secondResults.iterator().next();

        return nextEvents;
    }

    public static void deleteRecursive(final File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteRecursive(f);
            }
        }
        file.delete();
    }

}
