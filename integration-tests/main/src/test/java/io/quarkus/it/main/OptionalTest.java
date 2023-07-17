package io.quarkus.it.main;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class OptionalTest {

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of(Optional.empty()),
                Arguments.of(Optional.of("")),
                Arguments.of(Map.of("optional", Optional.empty())),
                Arguments.of(Map.of("optional", Optional.of(""))));
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void map(Object o) {

    }

}
