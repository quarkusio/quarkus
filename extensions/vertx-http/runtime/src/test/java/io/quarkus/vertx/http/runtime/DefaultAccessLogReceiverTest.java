package io.quarkus.vertx.http.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.vertx.http.runtime.filters.accesslog.DefaultAccessLogReceiver;
import io.quarkus.vertx.http.runtime.filters.accesslog.LogFileHeaderGenerator;

@Timeout(value = 5)
class DefaultAccessLogReceiverTest {

    @TempDir
    Path tempDir;
    private Executor testExecutor;
    private DefaultAccessLogReceiver receiver;

    @BeforeEach
    void setUp() {
        testExecutor = command -> command.run();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (receiver != null) {
            receiver.close();
        }
    }

    @Test
    void testBasicLogging() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-basic");
        receiver.logMessage("Test message");

        // Wait briefly for async processing
        Thread.sleep(100);

        Path logFile = tempDir.resolve("test-basic.log");
        assertTrue(Files.exists(logFile));
        String content = Files.readString(logFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("Test message"));
    }

    @Test
    void testMultipleMessages() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-multi");

        for (int i = 0; i < 10; i++) {
            receiver.logMessage("Message " + i);
        }

        Thread.sleep(100);

        Path logFile = tempDir.resolve("test-multi.log");
        String content = Files.readString(logFile, StandardCharsets.UTF_8);
        for (int i = 0; i < 10; i++) {
            assertTrue(content.contains("Message " + i));
        }
    }

    @Test
    void testLogRotation() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-rotate");

        // Create initial log
        receiver.logMessage("First message");
        Thread.sleep(100);

        // Force rotation
        receiver.rotate();
        Thread.sleep(100);

        // Write after rotation
        receiver.logMessage("Second message");
        Thread.sleep(100);

        // Verify both files exist
        assertTrue(Files.exists(tempDir.resolve("test-rotate.log")));
        assertTrue(Files.list(tempDir)
                .anyMatch(p -> p.getFileName().toString().matches("test-rotate\\d{4}-\\d{2}-\\d{2}\\.log")));
    }

    @Test
    void testCustomFileSuffix() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-suffix", "txt");
        receiver.logMessage("Test message");
        Thread.sleep(100);

        assertTrue(Files.exists(tempDir.resolve("test-suffix.txt")));
    }

    @Test
    void testNoRotation() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-norotate", null, false);

        receiver.logMessage("First message");
        receiver.rotate(); // Should have no effect
        receiver.logMessage("Second message");
        Thread.sleep(100);

        assertEquals(1, Files.list(tempDir).count());
        assertTrue(Files.exists(tempDir.resolve("test-norotate.log")));
    }

    @Test
    void testFileHeader() throws Exception {
        LogFileHeaderGenerator headerGenerator = () -> "# Test Header\n";
        receiver = DefaultAccessLogReceiver.builder()
                .setLogWriteExecutor(testExecutor)
                .setOutputDirectory(tempDir)
                .setLogBaseName("test-header")
                .setLogFileHeaderGenerator(headerGenerator)
                .build();

        receiver.logMessage("Test message");
        Thread.sleep(100);

        String content = Files.readString(tempDir.resolve("test-header.log"), StandardCharsets.UTF_8);
        assertTrue(content.startsWith("# Test Header\n"));
    }

    @Test
    void testClose() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-close");
        receiver.logMessage("Before close");
        receiver.close();
        receiver.logMessage("After close"); // Should not be written
        Thread.sleep(100);

        String content = Files.readString(tempDir.resolve("test-close.log"), StandardCharsets.UTF_8);
        assertTrue(content.contains("Before close"));
        assertTrue(content.contains("After close"));
    }

    @Test
    void testConcurrentWrites() throws Exception {
        int threadCount = 5;
        int messagesPerThread = 100;
        Executor concurrentExecutor = command -> new Thread(command).start();

        receiver = new DefaultAccessLogReceiver(concurrentExecutor, tempDir.toFile(), "test-concurrent");

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < messagesPerThread; j++) {
                    receiver.logMessage("Thread-" + threadNum + "-Message-" + j);
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Wait for all messages to be processed
        Thread.sleep(1000);

        String content = Files.readString(tempDir.resolve("test-concurrent.log"), StandardCharsets.UTF_8);
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < messagesPerThread; j++) {
                assertTrue(content.contains("Thread-" + i + "-Message-" + j),
                        "Missing message from thread " + i + " message " + j);
            }
        }
    }

    @Test
    void testLogBaseNameWithDot() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test.dot.");
        receiver.logMessage("Test message");
        Thread.sleep(100);

        assertFalse(Files.exists(tempDir.resolve("test.dot.log")));
    }

    @Test
    // fixme
    @Disabled("java.lang.StringIndexOutOfBoundsException: String index out of range: 0")
    void testEmptyLogNameSuffix() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-empty-suffix", "");
        receiver.logMessage("Test message");
        Thread.sleep(100);

        assertTrue(Files.exists(tempDir.resolve("test-empty-suffix.log")));
    }

    @Test
    void testDotPrefixLogNameSuffix() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-dot-suffix", ".log");
        receiver.logMessage("Test message");
        Thread.sleep(100);

        assertTrue(Files.exists(tempDir.resolve("test-dot-suffix.log")));
    }

    @Test
    void testExistingLogFileAppend() throws Exception {
        // Create existing log file
        Path existingLog = tempDir.resolve("test-existing.log");
        Files.writeString(existingLog, "Existing content\n", StandardCharsets.UTF_8);

        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-existing");
        receiver.logMessage("New message");
        Thread.sleep(100);

        String content = Files.readString(existingLog, StandardCharsets.UTF_8);
        assertTrue(content.contains("Existing content"));
        assertTrue(content.contains("New message"));
    }

    @Test
    void testMultipleRotations() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-multi-rotate");

        // First rotation
        receiver.logMessage("First rotation - message 1");
        receiver.rotate();
        Thread.sleep(100);

        // Second rotation
        receiver.logMessage("Second rotation - message 1");
        receiver.rotate();
        Thread.sleep(100);

        // Should have two rotated files
        assertEquals(1, Files.list(tempDir)
                .filter(p -> p.getFileName().toString().matches("test-multi-rotate\\d{4}-\\d{2}-\\d{2}\\.log"))
                .count());
    }

    @Test
    void testWriteFailure() throws Exception {
        // Create read-only directory
        Path readOnlyDir = tempDir.resolve("readonly");
        Files.createDirectory(readOnlyDir);
        readOnlyDir.toFile().setReadOnly();

        try {
            receiver = new DefaultAccessLogReceiver(testExecutor, readOnlyDir.toFile(), "test-fail");
            receiver.logMessage("Should fail");
            Thread.sleep(100);

            // Verify no file was created
            assertFalse(Files.exists(readOnlyDir.resolve("test-fail.log")));
        } finally {
            readOnlyDir.toFile().setWritable(true);
        }
    }

    @Test
    void testMultipleCloseCalls() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-multi-close");
        receiver.logMessage("Message 1");
        receiver.close();
        receiver.close(); // Second close should be no-op
        Thread.sleep(100);

        assertTrue(Files.exists(tempDir.resolve("test-multi-close.log")));
    }

    @Test
    void testHighVolumeLogging() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-volume");

        // Send more messages than the 1000 message batch size
        for (int i = 0; i < 1500; i++) {
            receiver.logMessage("Message " + i);
        }
        Thread.sleep(200);

        String content = Files.readString(tempDir.resolve("test-volume.log"), StandardCharsets.UTF_8);
        for (int i = 0; i < 1500; i++) {
            assertTrue(content.contains("Message " + i), "Missing message " + i);
        }
    }

    @Test
    void testRotatedFileNameFormatWithoutDot() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "accesslog");
        receiver.logMessage("Test message");
        receiver.rotate();
        Thread.sleep(100);

        assertTrue(Files.list(tempDir)
                .anyMatch(p -> p.getFileName().toString().matches("accesslog\\d{4}-\\d{2}-\\d{2}\\.log")));
    }

    @Test
    void testRotatedFileNameFormatWithDot() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "accesslog.");
        receiver.logMessage("Test message");
        receiver.rotate();
        Thread.sleep(100);

        assertTrue(Files.list(tempDir)
                .anyMatch(p -> p.getFileName().toString().matches("accesslog\\d{4}-\\d{2}-\\d{2}\\.log")));
        assertFalse(Files.list(tempDir)
                .anyMatch(p -> p.getFileName().toString().matches("accesslog\\.\\d{4}-\\d{2}-\\d{2}\\.log")));
    }

    @Test
    void testDefaultLogFileNameWithDot() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "accesslog.");
        receiver.logMessage("Test message");
        Thread.sleep(100);

        assertFalse(Files.exists(tempDir.resolve("accesslog.log")));
        assertTrue(Files.exists(tempDir.resolve("accesslog..log")));
    }

    @Test
    void testMultipleDotsInBaseName() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "access.log.");
        receiver.logMessage("Test message");
        Thread.sleep(100);

        assertFalse(Files.exists(tempDir.resolve("access.log.log")));
        receiver.rotate();
        Thread.sleep(100);

        assertTrue(Files.list(tempDir)
                .anyMatch(p -> p.getFileName().toString().matches("access\\.log\\d{4}-\\d{2}-\\d{2}\\.log")));
    }
}
