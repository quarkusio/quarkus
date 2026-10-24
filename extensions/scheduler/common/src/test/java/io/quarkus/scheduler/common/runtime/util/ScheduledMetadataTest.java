package io.quarkus.scheduler.common.runtime.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.common.runtime.ScheduledMetadata;

public class ScheduledMetadataTest {

    @Test
    public void testMetadataCoversAllScheduledMembers() {
        Set<String> members = new TreeSet<>();
        for (Method method : Scheduled.class.getDeclaredMethods()) {
            members.add(method.getName());
        }
        Set<String> components = new TreeSet<>();
        for (RecordComponent component : ScheduledMetadata.class.getRecordComponents()) {
            components.add(component.getName());
        }
        assertEquals(members, components);
    }

}
