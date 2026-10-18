package io.quarkus.vertx.http.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
