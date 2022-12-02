package io.quarkus.annotation.processor.generate_doc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigDocKeyTest {

    private ConfigDocKey configDocKey;

    @BeforeEach
    public void setup() {
        configDocKey = new ConfigDocKey();
    }

    @Test
    public void shouldComputePrimitiveSimpleName() {
        configDocKey.setType(int.class.getSimpleName());
        String simpleName = configDocKey.computeTypeSimpleName();
        assertEquals(int.class.getSimpleName(), simpleName);

        configDocKey.setType(long.class.getSimpleName());
        simpleName = configDocKey.computeTypeSimpleName();
        assertEquals(long.class.getSimpleName(), simpleName);

        configDocKey.setType(boolean.class.getSimpleName());
        simpleName = configDocKey.computeTypeSimpleName();
        assertEquals(boolean.class.getSimpleName(), simpleName);
    }

    @Test
    public void shouldComputeClassSimpleName() {
        configDocKey.setType(Duration.class.getName());
        String simpleName = configDocKey.computeTypeSimpleName();
        assertEquals(Duration.class.getSimpleName(), simpleName);

        configDocKey.setType(List.class.getName());
        simpleName = configDocKey.computeTypeSimpleName();
        assertEquals(List.class.getSimpleName(), simpleName);

        configDocKey.setType(String.class.getName());
        simpleName = configDocKey.computeTypeSimpleName();
        assertEquals(String.class.getSimpleName(), simpleName);

        configDocKey.setType(Map.Entry.class.getName());
        simpleName = configDocKey.computeTypeSimpleName();
        assertEquals(Map.Entry.class.getSimpleName(), simpleName);
    }
}
