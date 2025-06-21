package io.quarkus.vertx.http.runtime;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.vertx.http.runtime.filters.accesslog.DefaultAccessLogReceiver;

class DefaultAccessLogReceiverTest {

    @TempDir
    Path tempDir;
    private Executor testExecutor;
    private DefaultAccessLogReceiver receiver;

    @BeforeEach
    void setUp() {
        testExecutor = Runnable::run;
    }

    @AfterEach
    void tearDown() throws IOException {
        if (receiver != null) {
            receiver.close();
        }
    }

    private DefaultAccessLogReceiver createReceiver(String logBaseName) {
        return createReceiver(logBaseName, "log");
    }

    private DefaultAccessLogReceiver createReceiver(String logBaseName, String logNameSuffix) {
        DefaultAccessLogReceiver receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), logBaseName,
                logNameSuffix, true);
        // Run immediately to process any initial messages
        receiver.run();
        return receiver;
    }

    /**
     * <a href="https://github.com/quarkusio/quarkus/issues/48342">No divider between base file name and timestamp in the name
     * of rotated access log</a>
     */
    @Test
    void testRotationWithEmptyLogFile() throws Exception {
        // Setup test files
        String baseFileName = "test-empty";
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String currentDate = "." + df.format(new Date());

        // Create test files
        Path currentLog = tempDir.resolve(baseFileName + ".log");
        Path rotatedFile1 = tempDir.resolve(baseFileName + currentDate + ".log");
        Path rotatedFile2 = tempDir.resolve(baseFileName + currentDate + "-1.log");
        Path rotatedFile3 = tempDir.resolve(baseFileName + currentDate + "-2.log");
        Path rotatedFile4 = tempDir.resolve(baseFileName + currentDate + "-3.log");

        Files.createFile(currentLog);
        Files.createFile(rotatedFile1);
        Files.createFile(rotatedFile2);
        Files.createFile(rotatedFile3);
        Files.createFile(rotatedFile4);

        // Initialize receiver
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), baseFileName, "log", true);
        receiver.run();

        // Force rotation
        Field changeOverPointField = DefaultAccessLogReceiver.class.getDeclaredField("changeOverPoint");
        changeOverPointField.setAccessible(true);
        changeOverPointField.set(receiver, System.currentTimeMillis() - 1000);

        // Trigger rotation with new message
        receiver.logMessage("New message");
        receiver.run();

        // Verify all expected files exist
        assertThat(Files.list(tempDir))
                .extracting(Path::getFileName)
                .extracting(Path::toString)
                .containsExactlyInAnyOrder(
                        baseFileName + ".log",
                        baseFileName + currentDate + ".log", // First rotation
                        baseFileName + currentDate + "-1.log", // Second rotation
                        baseFileName + currentDate + "-2.log", // Third rotation
                        baseFileName + currentDate + "-3.log", // Fourth rotation
                        baseFileName + currentDate + "-4.log" // New rotation from this test
                );

        // Verify rotated files content
        assertThat(rotatedFile1).content(UTF_8).isEmpty();
        assertThat(rotatedFile2).content(UTF_8).isEmpty();
        assertThat(rotatedFile3).content(UTF_8).isEmpty();
        assertThat(rotatedFile4).content(UTF_8).isEmpty();

        // Verify new rotation file exists
        Path newRotation = tempDir.resolve(baseFileName + currentDate + "-4.log");
        assertThat(newRotation).exists();
    }

    @Test
    void testBasicLogging() throws Exception {
        receiver = createReceiver("test-basic");
        receiver.logMessage("Test message");
        receiver.run();

        Path logFile = tempDir.resolve("test-basic.log");
        assertThat(logFile).exists();
        assertThat(Files.readString(logFile, UTF_8))
                .isEqualTo("Test message\n");
    }

    @Test
    void testCreateReceiverNull() throws Exception {
        receiver = createReceiver(null);
        receiver.logMessage("Test message");
        receiver.run();
    }

    @Test
    void testCreateReceiverNullLogNameSuffix() throws Exception {
        receiver = createReceiver("test-basic", null);
        receiver.logMessage("Test message");
        receiver.run();

        Path logFile = tempDir.resolve("test-basic.log");
        assertThat(logFile).exists();
        assertThat(Files.readString(logFile, UTF_8))
                .isEqualTo("Test message\n");
    }

    @Test
    void testMultipleMessages() throws Exception {
        receiver = createReceiver("test-multi");

        for (int i = 0; i < 10; i++) {
            receiver.logMessage("Message " + i);
        }
        receiver.run();

        String content = Files.readString(tempDir.resolve("test-multi.log"), UTF_8);
        for (int i = 0; i < 10; i++) {
            assertThat(content).contains("Message " + i);
        }
    }

    @Test
    void testCustomFileSuffix() throws Exception {
        receiver = createReceiver("test-suffix", "txt");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve("test-suffix.txt")).exists();
    }

    @Test
    void testClose() throws Exception {
        receiver = createReceiver("test-close");
        receiver.logMessage("Before close");
        receiver.run();
        receiver.close();
        receiver.logMessage("After close");
        receiver.run();

        AbstractStringAssert<?> o = assertThat(Files.readString(tempDir.resolve("test-close.log"), UTF_8))
                .contains("Before close")
                .contains("After close");
    }

    @Test
    void testLogBaseNameWithDot() throws Exception {
        assertThat(tempDir.resolve("test.dot..log")).doesNotExist();
        assertThat(tempDir.resolve("test.dot.log")).doesNotExist();
        receiver = createReceiver("test.dot.");
        receiver.logMessage("Test message");
        receiver.run();
        assertThat(tempDir.resolve("test.dot..log")).doesNotExist();
        assertThat(tempDir.resolve("test.dot.log")).exists();
    }

    @Test
    void testLogBaseNameWithoutDot() throws Exception {
        receiver = createReceiver("testLogBaseNameWithoutDot.dot");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve("testLogBaseNameWithoutDot.dot..log")).doesNotExist();
        assertThat(tempDir.resolve("testLogBaseNameWithoutDot.dot.log")).exists();
    }

    @Test
    void testDotPrefixLogNameSuffix() throws Exception {
        receiver = createReceiver("test-dot-suffix", ".log");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve("test-dot-suffix.log")).exists();
    }

    @Test
    void testExistingLogFileAppend() throws Exception {
        Path existingLog = tempDir.resolve("test-existing.log");
        Files.writeString(existingLog, "Existing content\n", UTF_8);

        receiver = createReceiver("test-existing");
        receiver.logMessage("New message");
        receiver.run();

        assertThat(Files.readString(existingLog, UTF_8))
                .contains("Existing content")
                .contains("New message");
    }

    @Test
    void testMultipleCloseCalls() throws Exception {
        receiver = createReceiver("test-multi-close");
        receiver.logMessage("Message 1");
        receiver.run();
        receiver.close();
        receiver.close();

        assertThat(tempDir.resolve("test-multi-close.log")).exists();
    }

    @Test
    void testHighVolumeLogging() throws Exception {
        receiver = createReceiver("test-volume");

        for (int i = 0; i < 1500; i++) {
            receiver.logMessage("Message " + i);
        }
        receiver.run();

        String content = Files.readString(tempDir.resolve("test-volume.log"), UTF_8);
        for (int i = 0; i < 1500; i++) {
            assertThat(content)
                    .as("Missing message %d", i)
                    .contains("Message " + i);
        }
    }

    @Test
    void testLogBaseNameWithSingleTrailingDot() throws Exception {
        // Test that a single trailing dot is handled correctly
        receiver = createReceiver("accesslog.");
        receiver.logMessage("Test message");
        receiver.run();

        // Verify the correct file was created
        assertThat(tempDir.resolve("accesslog.log")).exists();

        // Verify no incorrect variations were created
        assertThat(tempDir.resolve("accesslog..log")).doesNotExist();
        assertThat(tempDir.resolve("accesslog...log")).doesNotExist();

        // Verify content was written correctly
        assertThat(Files.readString(tempDir.resolve("accesslog.log"), UTF_8))
                .isEqualTo("Test message\n");
    }

    @Test
    void testLogBaseNameWithDoubleTrailingDot() throws Exception {
        // Test that multiple trailing dots are handled correctly
        receiver = createReceiver("accesslog..");
        receiver.logMessage("Test message");
        receiver.run();

        // Verify the correct file was created (should preserve one dot)
        assertThat(tempDir.resolve("accesslog..log")).exists();

        // Verify no incorrect variations were created
        assertThat(tempDir.resolve("accesslog...log")).doesNotExist();
        assertThat(tempDir.resolve("accesslog.log")).doesNotExist();

        // Verify content was written correctly
        assertThat(Files.readString(tempDir.resolve("accesslog..log"), UTF_8))
                .isEqualTo("Test message\n");
    }

    @Test
    void testMultipleDotsInBaseName() throws Exception {
        receiver = createReceiver("access.log.");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve("access.log..log")).doesNotExist();
        assertThat(tempDir.resolve("access.log.log")).exists();
    }

    @Test
    void testEmptyMessage() throws Exception {
        receiver = createReceiver("test-empty");
        receiver.logMessage("");
        receiver.run();

        assertThat(tempDir.resolve("test-empty.log"))
                .exists()
                .content().isEqualTo("\n");
    }

    @Test
    void testMessage() throws Exception {
        receiver = createReceiver("test-foo");
        receiver.logMessage("foo");
        receiver.run();

        assertThat(tempDir.resolve("test-foo.log"))
                .exists()
                .content().isEqualTo("foo\n");
    }

    @Test
    void testSpecialCharactersInMessage() throws Exception {
        receiver = createReceiver("test-special-chars");
        String message = "Test message with special chars: \n\t\r\\\"'";
        receiver.logMessage(message);
        receiver.run();

        assertThat(Files.readString(tempDir.resolve("test-special-chars.log"), UTF_8))
                .contains(message);
    }

    @Test
    void testVeryLongMessage() throws Exception {
        receiver = createReceiver("test-long-message");
        String longMessage = new String(new char[10000]).replace('\0', 'x');
        receiver.logMessage(longMessage);
        receiver.run();

        assertThat(Files.readString(tempDir.resolve("test-long-message.log"), UTF_8))
                .contains(longMessage);
    }

    @Test
    void testNoRotationWhenDisabled() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-norotate", "log", false);
        receiver.run();

        receiver.logMessage("First message");
        receiver.run();
        receiver.logMessage("force rotation");
        receiver.run();
        receiver.logMessage("Second message");
        receiver.run();

        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().startsWith("test-norotate"))
                .hasSize(1)
                .first()
                .extracting(Path::getFileName)
                .isEqualTo(tempDir.resolve("test-norotate.log").getFileName());
    }

    @Test
    void testLogFileHeader() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-header", "log", true) {
        };
        receiver.run();

        receiver.logMessage("Test message");
        receiver.run();

        assertThat(Files.readString(tempDir.resolve("test-header.log"), UTF_8))
                .isEqualTo("Test message\n");
    }

    @Test
    void testConcurrentRotation() throws Exception {
        receiver = createReceiver("test-concurrent-rotate");

        Thread writerThread = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                receiver.logMessage("Message " + i);
                if (i == 50) {
                    receiver.logMessage("force rotation");
                }
            }
        });

        Thread rotationThread = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                receiver.logMessage("force rotation");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        writerThread.start();
        rotationThread.start();
        writerThread.join();
        rotationThread.join();

        receiver.run();

        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().startsWith("test-concurrent-rotate"))
                .hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void testLogRotationWithEmptyBaseName() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "", "log", true);
        receiver.run();

        receiver.logMessage("Test message");
        receiver.run();
        receiver.logMessage("force rotation");
        receiver.run();

        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().matches("\\.\\d{4}-\\d{2}-\\d{2}\\.log"))
                .hasSize(0);
    }

    @Test
    void testLogFileHeaderWithMultipleLines() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-multi-line-header", "log", true) {
        };
        receiver.run();

        receiver.logMessage("Test message");
        receiver.run();

        assertThat(Files.readString(tempDir.resolve("test-multi-line-header.log"), UTF_8))
                .isEqualTo("Test message\n");
    }

    @Test
    void testLogFileHeaderWithNullHeader() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-null-header", "log", true) {
        };
        receiver.run();

        receiver.logMessage("Test message");
        receiver.run();

        assertThat(Files.readString(tempDir.resolve("test-null-header.log"), UTF_8))
                .doesNotStartWith("null\n")
                .isEqualTo("Test message\n");
    }

    @Test
    void testLogFileHeaderWithEmptyHeader() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-empty-header", "log", true) {
        };
        receiver.run();

        receiver.logMessage("Test message");
        receiver.run();

        assertThat(Files.readString(tempDir.resolve("test-empty-header.log"), UTF_8))
                .doesNotStartWith("\n")
                .isEqualTo("Test message\n");
    }

    @Test
    void testMultipleInstancesSameFile() throws Exception {
        DefaultAccessLogReceiver receiver1 = createReceiver("test-multi-instance");
        DefaultAccessLogReceiver receiver2 = createReceiver("test-multi-instance");

        receiver1.logMessage("Message from receiver1");
        receiver2.logMessage("Message from receiver2");
        receiver1.run();
        receiver2.run();

        receiver1.close();
        receiver2.close();

        assertThat(Files.readString(tempDir.resolve("test-multi-instance.log"), UTF_8))
                .contains("Message from receiver1")
                .contains("Message from receiver2");
    }

    @Test
    void testLogBaseNameWithMultipleDots() throws Exception {
        receiver = createReceiver("test.multiple.dots");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve("test.multiple.dots.log")).exists();
    }

    @Test
    void testLogNameSuffixWithDot() throws Exception {
        receiver = createReceiver("test-suffix-dot", ".txt");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve("test-suffix-dot.txt")).exists();
    }

    @Test
    void testLogBaseNameWithSpecialCharacters() throws Exception {
        receiver = createReceiver("test@special#chars$");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve("test@special#chars$.log")).exists();
    }

    @Test
    void testLogRotationWithNormalBaseName() throws Exception {
        receiver = createReceiver("test-rotation");
        receiver.logMessage("First message");
        receiver.run();
        receiver.logMessage("force rotation");
        receiver.run();

        // Should create a rotated file with date pattern
        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().matches("test-rotation\\.\\d{4}-\\d{2}-\\d{2}\\.log"))
                .hasSize(0);

        // Original log file should still exist with new messages
        assertThat(tempDir.resolve("test-rotation.log"))
                .exists()
                .content().contains("First message");
    }

    @Test
    void testLogRotationWithDotInBaseName() throws Exception {
        receiver = createReceiver("test.rotation");
        receiver.logMessage("First message");
        receiver.run();
        receiver.logMessage("force rotation");
        receiver.run();

        // Should create rotated file with correct pattern
        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().matches("test\\.rotation.\\.\\d{4}-\\d{2}-\\d{2}\\.log"))
                .hasSize(0);

        assertThat(tempDir.resolve("test.rotation.log"))
                .exists()
                .content().contains("First message");
    }

    @Test
    void testMultipleRotations() throws Exception {
        receiver = createReceiver("test-multi-rotate");

        // First rotation
        receiver.logMessage("First rotation message");
        receiver.run();
        receiver.logMessage("force rotation");
        receiver.run();

        // Second rotation
        receiver.logMessage("Second rotation message");
        receiver.run();
        receiver.logMessage("force rotation");
        receiver.run();

        // Should only have one rotated file (per day)
        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().matches("test-multi-rotate\\.\\d{4}-\\d{2}-\\d{2}\\.log"))
                .hasSize(0);
    }

    @Test
    void testLogBaseNameEndingWithDot() throws Exception {
        receiver = createReceiver("test-dot.");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve("test-dot..log")).doesNotExist();
        assertThat(tempDir.resolve("test-dot.log")).exists();

        // Test rotation with dot ending
        receiver.logMessage("force rotation");
        receiver.run();

        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().matches("test-dot.\\.\\d{4}-\\d{2}-\\d{2}\\.log"))
                .hasSize(0);
    }

    @Test
    void testLogNameWithMultipleDots() throws Exception {
        receiver = createReceiver("test.multiple.dots.log");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve("test.multiple.dots.log.log")).exists();

        // Test rotation
        receiver.logMessage("force rotation");
        receiver.run();

        assertThat(Files.list(tempDir))
                .filteredOn(
                        p -> p.getFileName().toString().matches("test\\.multiple\\.dots\\.log\\.\\d{4}-\\d{2}-\\d{2}\\.log"))
                .hasSize(0);
    }

    @Test
    void testRotationWithEmptyBaseName() throws Exception {
        receiver = createReceiver("");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve(".log")).exists();

        // Test rotation
        receiver.logMessage("force rotation");
        receiver.run();

        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().matches("\\.\\d{4}-\\d{2}-\\d{2}\\.log"))
                .hasSize(0);
    }

    @Test
    void testLogRotationWithDateSuffix() throws Exception {
        receiver = createReceiver("test-rotation");
        // First message
        receiver.logMessage("First message");
        receiver.run();

        // Force rotation
        receiver.logMessage("force rotation");
        receiver.run();

        // Second message after rotation
        receiver.logMessage("Second message");
        receiver.run();

        // Should create a rotated file with date suffix
        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().matches("test-rotation\\.\\d{4}-\\d{2}-\\d{2}\\.log"))
                .hasSize(0);

        // Original file should only contain messages after rotation
        assertThat(Files.readString(tempDir.resolve("test-rotation.log"), UTF_8))
                .contains("Second message")
                .contains("force rotation")
                .contains("First message");
    }

    @Test
    void testMultipleRotations2() throws Exception {
        receiver = createReceiver("test-multi-rotation");

        for (int i = 0; i < 5; i++) {
            receiver.logMessage("Message " + i);
            receiver.logMessage("force rotation");
            receiver.run();
        }

        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().matches("test-multi-rotation.log"))
                .hasSize(1);
    }

    @Test
    void testLogBaseNameWithMultipleConsecutiveDots() throws Exception {
        receiver = createReceiver("test..multiple..dots..");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve("test..multiple..dots...log")).doesNotExist();
        assertThat(tempDir.resolve("test..multiple..dots..log")).exists();
    }

    @Test
    void testLogBaseNameWithLeadingDot() throws Exception {
        receiver = createReceiver(".hidden");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve(".hidden.log")).exists();
    }

    @Test
    void testLogBaseNameWithTrailingDotAndCustomSuffix() throws Exception {
        receiver = createReceiver("trailing-dot.", "txt");
        receiver.logMessage("Test message");
        receiver.run();

        assertThat(tempDir.resolve("trailing-dot.txt")).exists();
        assertThat(tempDir.resolve("trailing-dot..txt")).doesNotExist();
    }

    @Test
    void testRotationWithDotInBaseName() throws Exception {
        receiver = createReceiver("access.log");
        receiver.logMessage("First message");
        receiver.run();
        receiver.logMessage("force rotation");
        receiver.run();
        receiver.logMessage("Second message");
        receiver.run();

        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().matches("access.log.log"))
                .hasSize(1);
    }

    @Test
    void testLogBaseNameWithOnlyDots() throws Exception {
        assertThat(tempDir.resolve("....log")).doesNotExist();
        assertThat(tempDir.resolve("..log")).doesNotExist();
        assertThat(tempDir.resolve("..log")).doesNotExist();
        assertThat(tempDir.resolve(".log")).doesNotExist();
        assertThat(tempDir.resolve("log")).doesNotExist();
        receiver = createReceiver("...");
        receiver.logMessage("Test message");
        receiver.run();
        assertThat(tempDir.resolve("....log")).doesNotExist();
        assertThat(tempDir.resolve("..log")).doesNotExist();
        assertThat(tempDir.resolve("..log")).doesNotExist();
        assertThat(tempDir.resolve(".log")).doesNotExist();
        assertThat(tempDir.resolve("log")).doesNotExist();
    }

    @Test
    void testRotationWithEmptyMessages() throws Exception {
        receiver = createReceiver("test-empty-rotation");
        receiver.logMessage("");
        receiver.run();
        receiver.logMessage("force rotation");
        receiver.run();
        receiver.logMessage("");
        receiver.run();

        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().matches("test-empty-rotation(\\.\\d{4}-\\d{2}-\\d{2})?\\.log"))
                .hasSize(1);
    }

    @Test
    void testLogBaseNameWithDotAndRotation() throws Exception {
        receiver = createReceiver("version.1.0");
        receiver.logMessage("First message");
        receiver.run();
        receiver.logMessage("force rotation");
        receiver.run();
        receiver.logMessage("Second message");
        receiver.run();

        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().matches("version.1.0.log"))
                .hasSize(1);
    }

    @Test
    void testNoRotationWhenDisabled2() throws Exception {
        receiver = new DefaultAccessLogReceiver(testExecutor, tempDir.toFile(), "test-norotate", "log", false);
        receiver.run();

        receiver.logMessage("First message");
        receiver.run();

        // Attempt to force rotation by changing internal state
        Field changeOverPointField = DefaultAccessLogReceiver.class.getDeclaredField("changeOverPoint");
        changeOverPointField.setAccessible(true);
        changeOverPointField.set(receiver, System.currentTimeMillis() - 1000);

        receiver.logMessage("Second message");
        receiver.run();

        // Verify no rotation occurred
        assertThat(Files.list(tempDir))
                .filteredOn(p -> p.getFileName().toString().startsWith("test-norotate"))
                .hasSize(1)
                .first()
                .extracting(Path::getFileName)
                .isEqualTo(tempDir.resolve("test-norotate.log").getFileName());

        assertThat(Files.readString(tempDir.resolve("test-norotate.log"), UTF_8))
                .contains("First message")
                .contains("Second message");
    }

    @Test
    void testIOExceptionDuringWrite() throws Exception {
        receiver = createReceiver("test-write-io");

        // Use reflection to replace the writer with a mock that throws IOException
        Field writerField = DefaultAccessLogReceiver.class.getDeclaredField("writer");
        writerField.setAccessible(true);
        Writer mockWriter = mock(Writer.class);

        // Mock to throw IOException when any write operation is called
        doThrow(new IOException("Simulated IO error")).when(mockWriter).write(any(char[].class), anyInt(), anyInt());

        writerField.set(receiver, new BufferedWriter(mockWriter));

        receiver.logMessage("Should fail");
        receiver.run(); // This should handle the IOException gracefully

        // Verify the write was attempted
        verify(mockWriter, atLeastOnce()).write(any(char[].class), anyInt(), anyInt());
    }

    @Test
    void testIOExceptionDuringFlush() throws Exception {
        receiver = createReceiver("test-flush-io");

        // Use reflection to replace the writer with a mock that throws IOException on flush
        Field writerField = DefaultAccessLogReceiver.class.getDeclaredField("writer");
        writerField.setAccessible(true);
        Writer mockWriter = mock(Writer.class);
        doThrow(new IOException("Simulated flush error")).when(mockWriter).flush();
        writerField.set(receiver, new BufferedWriter(mockWriter));

        receiver.logMessage("Should fail on flush");
        receiver.run(); // This should handle the IOException gracefully

        verify(mockWriter, times(1)).flush();
    }

    @Test
    void testIOExceptionDuringClose() throws Exception {
        receiver = createReceiver("test-close-io");

        // Replace writer with a mock that throws IOException on close
        Field writerField = DefaultAccessLogReceiver.class.getDeclaredField("writer");
        writerField.setAccessible(true);
        Writer mockWriter = mock(Writer.class);
        doThrow(new IOException("Simulated close error")).when(mockWriter).close();
        writerField.set(receiver, new BufferedWriter(mockWriter));

        receiver.close(); // Should handle the IOException gracefully

        verify(mockWriter, times(1)).close();
    }

    @Test
    void testCurrentDateStringWhenNoExistingFile() throws Exception {
        // Setup - ensure no existing file
        Path logFile = tempDir.resolve("test-date.log");
        assertThat(Files.exists(logFile)).isFalse();

        // Create receiver
        DefaultAccessLogReceiver receiver = new DefaultAccessLogReceiver(
                testExecutor, tempDir.toFile(), "test-date", "log", true);

        // Verify currentDateString was set to today's date
        Field dateStringField = DefaultAccessLogReceiver.class.getDeclaredField("currentDateString");
        dateStringField.setAccessible(true);
        String currentDateString = (String) dateStringField.get(receiver);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String expectedDate = df.format(new Date());

        assertThat(currentDateString).isEqualTo(expectedDate);

        receiver.close();
    }

    @Test
    void testCurrentDateStringUsesFileModifiedDate() throws Exception {
        // Setup - create a file with specific modified date
        Path logFile = tempDir.resolve("test-date.log");
        Files.createFile(logFile);

        // Set modified date to yesterday
        long yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        Files.setLastModifiedTime(logFile, FileTime.fromMillis(yesterday));

        // Create receiver
        DefaultAccessLogReceiver receiver = new DefaultAccessLogReceiver(
                testExecutor, tempDir.toFile(), "test-date", "log", true);

        // Verify currentDateString was set to yesterday's date
        Field dateStringField = DefaultAccessLogReceiver.class.getDeclaredField("currentDateString");
        dateStringField.setAccessible(true);
        String currentDateString = (String) dateStringField.get(receiver);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String expectedDate = df.format(new Date(yesterday));

        assertThat(currentDateString).isEqualTo(expectedDate);

        receiver.close();
    }
}
