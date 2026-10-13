package io.quarkus.kubernetes.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests to verify Map.Entry sorting behavior used throughout the kubernetes extension
 * to ensure deterministic manifest generation. Uses JUnit 5.
 */
@DisplayName("Map Entry Sorting Tests")
class MapSortingTest {

    @Test
    @DisplayName("Should sort map entries alphabetically by key")
    void testBasicSorting() {
        Map<String, String> map = new HashMap<>();
        map.put("zebra", "1");
        map.put("alpha", "2");
        map.put("mike", "3");

        List<String> sorted = map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .toList();

        assertEquals(List.of("alpha", "mike", "zebra"), sorted);
    }

    @Test
    @DisplayName("Should handle empty map")
    void testEmptyMap() {
        Map<String, String> map = new HashMap<>();

        List<String> sorted = map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .toList();

        assertTrue(sorted.isEmpty());
    }

    @Test
    @DisplayName("Should handle single entry")
    void testSingleEntry() {
        Map<String, String> map = new HashMap<>();
        map.put("only", "value");

        List<String> sorted = map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .toList();

        assertEquals(List.of("only"), sorted);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideSortingScenarios")
    @DisplayName("Various sorting scenarios")
    void testVariousScenarios(String scenario, Map<String, String> input, List<String> expected) {
        List<String> sorted = input.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .toList();

        assertEquals(expected, sorted);
    }

    static Stream<Arguments> provideSortingScenarios() {
        return Stream.of(
                Arguments.of("Port names",
                        Map.of("https", "8443", "http", "8080", "admin", "9090"),
                        List.of("admin", "http", "https")),
                Arguments.of("Env var names",
                        Map.of("VAR_Z", "z", "VAR_A", "a", "VAR_M", "m"),
                        List.of("VAR_A", "VAR_M", "VAR_Z")),
                Arguments.of("Label keys",
                        Map.of("team", "platform", "env", "prod", "app", "api"),
                        List.of("app", "env", "team")),
                Arguments.of("Annotation keys with dots",
                        Map.of("prometheus.io/scrape", "true", "app.io/version", "1.0"),
                        List.of("app.io/version", "prometheus.io/scrape")),
                Arguments.of("Volume names",
                        Map.of("secret-vol", "secret", "config-vol", "cm", "data-vol", "pvc"),
                        List.of("config-vol", "data-vol", "secret-vol")));
    }

    @Test
    @DisplayName("Should be consistent across multiple sorts")
    void testConsistency() {
        Map<String, String> map = new HashMap<>();
        map.put("c", "3");
        map.put("a", "1");
        map.put("b", "2");

        List<String> sort1 = map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .toList();

        List<String> sort2 = map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .toList();

        assertEquals(sort1, sort2);
    }

    @Test
    @DisplayName("Should handle keys with special characters")
    void testSpecialCharacters() {
        Map<String, String> map = new HashMap<>();
        map.put("key-with-dashes", "1");
        map.put("key.with.dots", "2");
        map.put("key_with_underscores", "3");
        map.put("key/with/slashes", "4");

        List<String> sorted = map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .toList();

        // Verify sorted
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).compareTo(sorted.get(i + 1)) <= 0, "List should be sorted");
        }
        assertEquals(4, sorted.size());
    }

    @Test
    @DisplayName("Should handle numeric suffixes lexicographically")
    void testNumericSuffixes() {
        Map<String, String> map = new HashMap<>();
        map.put("port-10", "1");
        map.put("port-2", "2");
        map.put("port-1", "3");

        List<String> sorted = map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .toList();

        // Lexicographic: "1" < "10" < "2"
        assertEquals(List.of("port-1", "port-10", "port-2"), sorted);
    }

    @Test
    @DisplayName("Should handle null values without affecting sort order")
    void testNullValues() {
        Map<String, String> map = new HashMap<>();
        map.put("z", null);
        map.put("a", "value");
        map.put("m", null);

        List<String> sorted = map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .toList();

        assertEquals(List.of("a", "m", "z"), sorted);
    }

    @Test
    @DisplayName("Should handle large maps efficiently")
    void testLargeMap() {
        Map<String, String> map = new HashMap<>();
        for (int i = 100; i >= 0; i--) {
            map.put("key-" + i, "value-" + i);
        }

        List<String> sorted = map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getKey)
                .toList();

        assertEquals(101, sorted.size());
        // Verify sorted
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).compareTo(sorted.get(i + 1)) <= 0, "List should be sorted");
        }
    }
}
