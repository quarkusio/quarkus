package io.quarkus.kotlin.serialization;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import kotlinx.serialization.json.JsonConfiguration;

public class JsonConfigTest {
    @Test
    public void ensureJsonCoverage() {
        Set<String> kotlinFields = new TreeSet<>(Arrays.stream(JsonConfiguration.class.getDeclaredFields())
                .map(f -> f.getName())
                .collect(Collectors.toSet()));
        kotlinFields.removeAll(Arrays.stream(JsonConfig.class.getDeclaredFields())
                .map(f -> f.getName())
                .collect(Collectors.toSet()));
        assertTrue(kotlinFields.isEmpty(), "Should find all the Kotlin fields on the quarkus config object. " +
                "missing elements: " + kotlinFields);
    }
}
