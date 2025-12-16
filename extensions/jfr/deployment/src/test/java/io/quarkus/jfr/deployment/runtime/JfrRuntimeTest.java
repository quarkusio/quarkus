package io.quarkus.jfr.deployment.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.jfr.runtime.internal.runtime.QuarkusRuntimeInfo;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.test.QuarkusUnitTest;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

public class JfrRuntimeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    QuarkusRuntimeInfo quarkusRuntimeInfo;

    @Inject
    ApplicationConfig applicationConfig;

    @Test
    public void test() throws IOException {
        final Path dumpPath = Path.of("./dump.jfr");
        try {
            try (Recording r = new Recording()) {
                r.start();
                r.stop();
                r.dump(dumpPath);
            } catch (Exception e) {
                Assertions.fail(e);
            }
            List<RecordedEvent> recordedEvents = RecordingFile.readAllEvents(dumpPath);

            testApplicationEvent(recordedEvents);
            testRuntimeEvent(recordedEvents);
            testExtensionEvent(recordedEvents);
        } finally {
            if (Files.exists(dumpPath)) {
                Files.delete(dumpPath);
            }
        }
    }

    private void testApplicationEvent(List<RecordedEvent> recordedEvents) {
        List<RecordedEvent> applicationEvents = recordedEvents.stream()
                .filter(e -> e.getEventType().getName().equals("quarkus.application")).toList();
        RecordedEvent applicationEvent = applicationEvents.get(0);
        Assertions.assertTrue(applicationEvents.size() >= 1);
        Assertions.assertEquals(applicationConfig.name().get(), applicationEvent.getString("name"));
        Assertions.assertEquals(applicationConfig.version().get(), applicationEvent.getString("version"));
    }

    private void testRuntimeEvent(List<RecordedEvent> recordedEvents) {
        List<RecordedEvent> runtimeEvents = recordedEvents.stream()
                .filter(e -> e.getEventType().getName().equals("quarkus.runtime")).toList();
        RecordedEvent runtimeEvent = runtimeEvents.get(0);
        Assertions.assertTrue(runtimeEvents.size() >= 1);
        Assertions.assertEquals(quarkusRuntimeInfo.imageMode(), runtimeEvent.getString("imageMode"));
        Assertions.assertEquals(quarkusRuntimeInfo.version(), runtimeEvent.getString("version"));
        Assertions.assertEquals(quarkusRuntimeInfo.profiles(), runtimeEvent.getString("profiles"));
    }

    private void testExtensionEvent(List<RecordedEvent> recordedEvents) {
        List<RecordedEvent> extensionEvents = recordedEvents.stream()
                .filter(e -> e.getEventType().getName().equals("quarkus.extension")).toList();
        List<String> recordedExtensionNames = extensionEvents.stream().map(e -> e.getString("name")).distinct().toList();
        Assertions.assertEquals(quarkusRuntimeInfo.features().size(), recordedExtensionNames.size());
        Assertions.assertTrue(recordedExtensionNames.containsAll(quarkusRuntimeInfo.features()));
    }
}
