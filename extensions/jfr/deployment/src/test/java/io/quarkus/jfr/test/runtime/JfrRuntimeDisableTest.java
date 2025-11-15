package io.quarkus.jfr.test.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

public class JfrRuntimeDisableTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideRuntimeConfigKey("quarkus.jfr.runtime.enabled", "false");

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
            Assertions.assertEquals(0, recordedEvents.size());
        } finally {
            if (Files.exists(dumpPath)) {
                Files.delete(dumpPath);
            }
        }
    }
}
