package io.quarkus.gradle.model.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TaskInputFingerprintTest {

    @Test
    void mapFingerprintIsOrderIndependentAndValueSensitive() {
        Map<String, String> first = new LinkedHashMap<>();
        first.put("one", "1");
        first.put("two", "2");
        Map<String, String> reordered = new LinkedHashMap<>();
        reordered.put("two", "2");
        reordered.put("one", "1");

        assertThat(TaskInputFingerprint.ofMap(first))
                .isEqualTo(TaskInputFingerprint.ofMap(reordered))
                .isNotEqualTo(TaskInputFingerprint.ofMap(Map.of("one", "changed", "two", "2")));
    }
}
