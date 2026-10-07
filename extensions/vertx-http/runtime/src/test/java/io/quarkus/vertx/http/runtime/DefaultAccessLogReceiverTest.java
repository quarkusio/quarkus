package io.quarkus.vertx.http.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.vertx.http.runtime.filters.accesslog.DefaultAccessLogReceiver;

class DefaultAccessLogReceiverTest {

    @TempDir
    Path tempDir;

    @Test
    void testBaseFileName() throws IOException {
        String logBaseName = "test-base-file-name";
        validateLogFileNames(logBaseName, null);
    }

    @Test
    void testBaseFileNameWithTrailingDot() throws IOException {
        String logBaseName = "test-base-file-name-with-trailing-dot.";
        validateLogFileNames(logBaseName, null);
    }

    @Test
    void testBaseFileNameWithTrailingDotAndSuffix() throws IOException {
        String logBaseName = "test-base-file-name-with-trailing-dot-and-suffix.";
        validateLogFileNames(logBaseName, "suffix");
    }

    @Test
    void testBaseFileNameWithTrailingDotAndSuffixWithLeadingDot() throws IOException {
        String logBaseName = "test-base-file-name-with-trailing-dot-and-suffix-with-leading-dot.";
        validateLogFileNames(logBaseName, ".suffix");
    }

    @Test
    void testMaxBackupIndexPrunesTheOldestRotatedFiles() throws IOException {
        Path unrelatedDated = Files.writeString(tempDir.resolve("other.2020-01-01.log"), "keep");
        Path unrelatedSuffix = Files.writeString(tempDir.resolve("access.log.bak"), "keep");
        DefaultAccessLogReceiver receiver = DefaultAccessLogReceiver.builder()
                .setLogWriteExecutor(Runnable::run)
                .setOutputDirectory(tempDir)
                .setLogBaseName("access")
                .setLogNameSuffix("log")
                .setRotate(true)
                .setMaxBackupIndex(2)
                .build();
        try (receiver) {
            for (int i = 1; i <= 5; i++) {
                receiver.logMessage("Message " + i);
                receiver.run();
                receiver.rotate();
                receiver.run();
            }
            receiver.logMessage("Message 6");
            receiver.run();

            Pattern rotated = Pattern.compile("access\\.[0-9]{4}-[0-9]{2}-[0-9]{2}(-[0-9]+)?\\.log");
            List<Path> rotatedFiles = Files.list(tempDir)
                    .filter(p -> rotated.matcher(p.getFileName().toString()).matches()).toList();
            assertThat(rotatedFiles).hasSize(2);
            assertThat(rotatedFiles).map(p -> Files.readString(p).trim())
                    .containsExactlyInAnyOrder("Message 4", "Message 5");
            assertThat(tempDir.resolve("access.log")).content().isEqualTo("Message 6" + System.lineSeparator());
            assertThat(unrelatedDated).exists();
            assertThat(unrelatedSuffix).exists();
        }
    }

    @Test
    void testRotatedFilesAreKeptWithoutMaxBackupIndex() throws IOException {
        try (DefaultAccessLogReceiver receiver = new DefaultAccessLogReceiver(Runnable::run, tempDir, "access", "log",
                true)) {
            for (int i = 1; i <= 3; i++) {
                receiver.logMessage("Message " + i);
                receiver.run();
                receiver.rotate();
                receiver.run();
            }
            assertThat(Files.list(tempDir)).hasSize(3);
        }
    }

    private void validateLogFileNames(String logBaseName, String logNameSuffix) throws IOException {
        String normalizedLogBaseName = logBaseName.endsWith(".") ? logBaseName.substring(0, logBaseName.length() - 1)
                : logBaseName;
        String normalizedLogNameSuffix = logNameSuffix == null ? "log"
                : (logNameSuffix.startsWith(".") ? logNameSuffix.substring(1) : logNameSuffix);

        // sanity checks
        assertThat(normalizedLogBaseName).doesNotEndWith(".");
        assertThat(normalizedLogNameSuffix).doesNotStartWith(".");

        try (DefaultAccessLogReceiver receiver = new DefaultAccessLogReceiver(Runnable::run, tempDir.toFile(), logBaseName,
                logNameSuffix, true)) {
            receiver.run();
            receiver.rotate();
            receiver.logMessage("Message 1");
            receiver.run();
            receiver.rotate();
            receiver.logMessage("Message 2");
            receiver.run();
            receiver.rotate();
            receiver.logMessage("Message 3");
            receiver.run();

            assertThat(Files.list(tempDir))
                    .extracting(Path::getFileName)
                    .extracting(Path::toString)
                    .satisfiesExactlyInAnyOrder(
                            item1 -> assertThat(item1).isEqualTo(normalizedLogBaseName + "." + normalizedLogNameSuffix),
                            item2 -> assertThat(item2).matches(Pattern.compile(Pattern.quote(normalizedLogBaseName)
                                    + "\\.[0-9]{4}-[0-9]{2}-[0-9]{2}" + "(-[0-9])?\\." + normalizedLogNameSuffix)),
                            item3 -> assertThat(item3).matches(Pattern.compile(Pattern.quote(normalizedLogBaseName)
                                    + "\\.[0-9]{4}-[0-9]{2}-[0-9]{2}" + "(-[0-9])?\\." + normalizedLogNameSuffix)));
        }
    }
}
