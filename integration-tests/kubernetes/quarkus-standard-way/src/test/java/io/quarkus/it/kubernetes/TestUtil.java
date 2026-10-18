package io.quarkus.it.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class TestUtil {

    private TestUtil() {
    }

    public static void assertLogFileContents(Path logfile, String... expectedOutput) {
        // the logs might not be flushed to disk immediately, so wait a few seconds before giving up completely
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            List<String> elements = Collections.emptyList();
            try {
                elements = Files.readAllLines(logfile);
            } catch (IOException ignored) {

            }
            final String entireLogContent = String.join("\n", elements);
            assertThat(entireLogContent).contains(expectedOutput);
        });
    }
}
