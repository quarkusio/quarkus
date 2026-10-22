package io.quarkus.scheduler.common.runtime.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.common.runtime.ScheduledMetadata;

public class ScheduledMetadataTest {

    @Scheduled(every = "1s", identity = "a", description = "d")
    void annotated() {
    }

    @Scheduled(cron = "0 0 * * * ?")
    void other() {
    }

    @Test
    public void testEqualsAndHashCodeFollowAnnotationContract() throws Exception {
        Scheduled annotation = ScheduledMetadataTest.class.getDeclaredMethod("annotated").getAnnotation(Scheduled.class);
        ScheduledMetadata metadata = copyOf(annotation);

        assertEquals(annotation, metadata);
        assertEquals(metadata, annotation);
        assertEquals(annotation.hashCode(), metadata.hashCode());
        assertTrue(Set.of(annotation).contains(metadata));
    }

    @Test
    public void testDifferentMembersAreNotEqual() throws Exception {
        Scheduled annotation = ScheduledMetadataTest.class.getDeclaredMethod("annotated").getAnnotation(Scheduled.class);
        Scheduled other = ScheduledMetadataTest.class.getDeclaredMethod("other").getAnnotation(Scheduled.class);

        assertNotEquals(copyOf(annotation), other);
        assertNotEquals(copyOf(annotation), copyOf(other));
    }

    private static ScheduledMetadata copyOf(Scheduled s) {
        return new ScheduledMetadata(s.identity(), s.cron(), s.every(), s.delay(), s.delayUnit(), s.delayed(),
                s.overdueGracePeriod(), s.concurrentExecution(), s.skipExecutionIf(), s.timeZone(), s.executeWith(),
                s.executionMaxDelay(), s.description());
    }
}
