package io.quarkus.jfr.deployment.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.test.ProdBuildResults;
import io.quarkus.test.ProdModeTestResults;
import io.quarkus.test.QuarkusProdModeTest;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/**
 * Verifies that Quarkus JFR events are present in a dumponexit recording.
 * <p>
 * JFR's dumponexit hook and Quarkus' shutdown hook are both JVM shutdown hooks
 * that run concurrently with no ordering guarantee. If the Quarkus hook runs first, the events will be unregistered
 * before JFR has a chance to emit them a final time. {@code JfrRuntimeBean.disable()}
 * calls {@code emitEvents()} before unregistering to ensure events are committed
 * to the JFR buffer regardless of which hook wins the race.
 */
public class JfrEmitOnExitTest {

    private static final String DUMP_FILENAME = "emit-on-exit-test.jfr";

    @RegisterExtension
    static final QuarkusProdModeTest app = new QuarkusProdModeTest()
            .withApplicationRoot(jar -> jar.addClass(ExitImmediately.class))
            .setApplicationName("jfr-emit-on-exit-test")
            .setExpectExit(true) // Wait for child process to exit
            .setRun(true)
            .setJVMArgs(List.of("-XX:StartFlightRecording:dumponexit=true,filename=" + DUMP_FILENAME));

    @ProdBuildResults
    private ProdModeTestResults prodModeTestResults;

    @Test
    public void dumpOnExitContainsQuarkusEvents() throws IOException {
        Path dumpPath = prodModeTestResults.getBuiltArtifactPath().getParent().resolve(DUMP_FILENAME);
        assertTrue(Files.exists(dumpPath), "JFR dump file should exist after shutdown");
        dumpPath.toFile().deleteOnExit();

        List<RecordedEvent> events = RecordingFile.readAllEvents(dumpPath);

        List<RecordedEvent> runtimeEvents = events.stream()
                .filter(e -> e.getEventType().getName().equals("quarkus.runtime"))
                .toList();
        assertFalse(runtimeEvents.isEmpty(),
                "recording should contain quarkus.runtime events");

        List<RecordedEvent> applicationEvents = events.stream()
                .filter(e -> e.getEventType().getName().equals("quarkus.application"))
                .toList();
        assertFalse(applicationEvents.isEmpty(),
                "recording should contain quarkus.application events");

        List<RecordedEvent> extensionEvents = events.stream()
                .filter(e -> e.getEventType().getName().equals("quarkus.extension"))
                .toList();
        assertFalse(extensionEvents.isEmpty(),
                "recording should contain quarkus.extension events");
    }

    @QuarkusMain
    public static class ExitImmediately implements QuarkusApplication {
        @Override
        public int run(String... args) {
            return 0;
        }
    }
}
