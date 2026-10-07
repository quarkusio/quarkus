package io.quarkus.core.impl;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SortedNullSafeMapTest {

    @Test
    void ofNoArgsReturnsEmptyMap() {
        Map<String, String> map = SortedNullSafeMap.of();

        assertThat(map).isEmpty();
        assertUnmodifiable(map);
    }

    @Test
    void ofSingleEntryKeepsNonNullValue() {
        Map<String, Integer> map = SortedNullSafeMap.of("a", 1);

        assertThat(map).containsExactly(entry("a", 1));
        assertUnmodifiable(map);
    }

    @Test
    void ofSingleEntryWithNullValueReturnsEmptyMap() {
        Map<String, Integer> map = SortedNullSafeMap.of("a", null);

        assertThat(map).isEmpty();
    }

    @Test
    void ofTwoEntriesKeepsBothNonNullValues() {
        Map<String, Integer> map = SortedNullSafeMap.of("a", 1, "b", 2);

        assertThat(map).containsExactly(entry("a", 1), entry("b", 2));
        assertUnmodifiable(map);
    }

    @Test
    void ofTwoEntriesSkipsNullValues() {
        assertThat(SortedNullSafeMap.of("a", null, "b", 2)).containsExactly(entry("b", 2));
        assertThat(SortedNullSafeMap.of("a", 1, "b", null)).containsExactly(entry("a", 1));
        assertThat(SortedNullSafeMap.<Integer> of("a", null, "b", null)).isEmpty();
    }

    @Test
    void ofThreeEntriesKeepsNonNullValues() {
        Map<String, Integer> map = SortedNullSafeMap.of("a", 1, "b", 2, "c", 3);

        assertThat(map).containsExactly(entry("a", 1), entry("b", 2), entry("c", 3));
        assertUnmodifiable(map);
    }

    @Test
    void ofThreeEntriesSkipsNullValues() {
        assertThat(SortedNullSafeMap.of("a", null, "b", 2, "c", 3))
                .containsExactly(entry("b", 2), entry("c", 3));
        assertThat(SortedNullSafeMap.of("a", 1, "b", null, "c", 3))
                .containsExactly(entry("a", 1), entry("c", 3));
        assertThat(SortedNullSafeMap.of("a", 1, "b", 2, "c", null))
                .containsExactly(entry("a", 1), entry("b", 2));
        assertThat(SortedNullSafeMap.<Integer> of("a", null, "b", null, "c", null)).isEmpty();
    }

    @Test
    void ofFourEntriesKeepsNonNullValues() {
        Map<String, Integer> map = SortedNullSafeMap.of("a", 1, "b", 2, "c", 3, "d", 4);

        assertThat(map).containsExactly(entry("a", 1), entry("b", 2), entry("c", 3), entry("d", 4));
        assertUnmodifiable(map);
    }

    @Test
    void ofFourEntriesSkipsNullValues() {
        assertThat(SortedNullSafeMap.of("a", null, "b", 2, "c", 3, "d", 4))
                .containsExactly(entry("b", 2), entry("c", 3), entry("d", 4));
        assertThat(SortedNullSafeMap.of("a", 1, "b", null, "c", 3, "d", null))
                .containsExactly(entry("a", 1), entry("c", 3));
        assertThat(SortedNullSafeMap.<Integer> of("a", null, "b", null, "c", null, "d", null)).isEmpty();
    }

    @Test
    void ofEntriesWithEmptyArrayReturnsEmptyMap() {
        Map<String, String> map = SortedNullSafeMap.ofEntries();

        assertThat(map).isEmpty();
        assertUnmodifiable(map);
    }

    @Test
    void ofEntriesWithOddNumberOfArgumentsThrows() {
        assertThatThrownBy(() -> SortedNullSafeMap.ofEntries("a", 1, "b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Odd number");
    }

    @Test
    void ofEntriesSkipsNullValuesAndSortsKeys() {
        Map<String, Integer> map = SortedNullSafeMap.ofEntries("c", 3, "a", null, "b", 2);

        assertThat(map).containsExactly(entry("b", 2), entry("c", 3));
        assertUnmodifiable(map);
    }

    @Test
    void ofEntriesWithAllNullValuesReturnsEmptyMap() {
        Map<String, Integer> map = SortedNullSafeMap.ofEntries("a", null, "b", null);

        assertThat(map).isEmpty();
    }

    private static <V> void assertUnmodifiable(Map<String, V> map) {
        assertThatThrownBy(() -> map.put("new-key", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
