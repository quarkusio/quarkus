package io.quarkus.paths;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PathFilterTest {

    static Stream<Arguments> equalsAndHashCode() {
        return Stream.of(
                Arguments.of(List.of(), List.of()),
                Arguments.of(List.of("a*"), List.of("b*")),
                Arguments.of(List.of("/a", "b"), List.of("/c")),
                Arguments.of(List.of("/a", "b"), null),
                Arguments.of(null, List.of("/a", "b")),
                Arguments.of(List.of(), null),
                Arguments.of(null, List.of()));
    }

    @ParameterizedTest
    @MethodSource
    void equalsAndHashCode(List<String> includes, List<String> excludes) {
        PathFilter f0 = new PathFilter(includes, excludes);
        PathFilter f1 = new PathFilter(includes, excludes);
        assertThat(f0).isEqualTo(f1);
        assertThat(f0).hasSameHashCodeAs(f1);
        assertThat(f0).isEqualTo(PathFilter.fromMap(f0.asMap()));
        assertThat(f0).hasSameHashCodeAs(PathFilter.fromMap(f0.asMap()));
    }

    @Test
    void patternOrder() {
        PathFilter f0 = new PathFilter(List.of("a", "b"), List.of("c", "d"));
        PathFilter f1 = new PathFilter(List.of("b", "a"), List.of("d", "c"));
        assertThat(f0).isEqualTo(f1);
        assertThat(f0).hasSameHashCodeAs(f1);
        assertThat(f0).isEqualTo(PathFilter.fromMap(f1.asMap()));
        assertThat(f0).hasSameHashCodeAs(PathFilter.fromMap(f1.asMap()));
    }
}
