package io.quarkus.annotation.processor.generate_doc;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class ConfigItemTest {

    private ConfigItem configItem;

    @Before
    public void setup() {
        configItem = new ConfigItem();
    }

    @Test
    public void shouldComputePrimitiveSimpleName() {
        configItem.setType(int.class.getSimpleName());
        String simpleName = configItem.computeTypeSimpleName();
        assertEquals(int.class.getSimpleName(), simpleName);

        configItem.setType(long.class.getSimpleName());
        simpleName = configItem.computeTypeSimpleName();
        assertEquals(long.class.getSimpleName(), simpleName);

        configItem.setType(boolean.class.getSimpleName());
        simpleName = configItem.computeTypeSimpleName();
        assertEquals(boolean.class.getSimpleName(), simpleName);
    }

    @Test
    public void shouldComputeClassSimpleName() {
        configItem.setType(Duration.class.getName());
        String simpleName = configItem.computeTypeSimpleName();
        assertEquals(Duration.class.getSimpleName(), simpleName);

        configItem.setType(List.class.getName());
        simpleName = configItem.computeTypeSimpleName();
        assertEquals(List.class.getSimpleName(), simpleName);

        configItem.setType(String.class.getName());
        simpleName = configItem.computeTypeSimpleName();
        assertEquals(String.class.getSimpleName(), simpleName);

        configItem.setType(Map.Entry.class.getName());
        simpleName = configItem.computeTypeSimpleName();
        assertEquals(Map.Entry.class.getSimpleName(), simpleName);
    }
}
