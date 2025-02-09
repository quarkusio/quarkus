package io.quarkus.deployment.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jboss.jandex.Type;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AsmUtilTest {

    @ParameterizedTest
    @MethodSource
    void testGetParameterTypes(String methodDescriptor, String... expected) {
        assertArrayEquals(expected,
                Stream.of(AsmUtil.getParameterTypes(methodDescriptor)).map(Type::toString).toArray(String[]::new));
    }

    private static final Stream<Arguments> testGetParameterTypes() {
        List<Arguments> arguments = new ArrayList<>();
        final var array1 = new StringBuilder();
        final var array2 = new StringBuilder();
        for (int i = 0;i < 5;i++) {
            for (Character c : AsmUtil.PRIMITIVE_DESCRIPTOR_TO_PRIMITIVE_CLASS_LITERAL.keySet()) {
                arguments.add(Arguments.of("(" + array2 + c + ")V",
                        toArray(AsmUtil.PRIMITIVE_DESCRIPTOR_TO_PRIMITIVE_CLASS_LITERAL.get(c) + array1)));
            }
            array1.append("[]");
            array2.append('[');
        }
        return arguments.stream();
    }

    private static String[] toArray(String... values) {
        return values;
    }
}
