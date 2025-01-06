package io.quarkus.deployment;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class CapabilityTest {
    private static Stream<Arguments> capabilityFields() {
        Field[] declaredFields = Capability.class.getDeclaredFields();
        return Stream.of(declaredFields)
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> field.getType().equals(String.class))
                .map(CapabilityTest::getString)
                .filter(not(Capability.QUARKUS_PREFIX::equals))
                .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("capabilityFields")
    void validateCapabilityString(String capabilityName) {
        assertThat(capabilityName).startsWith(Capability.QUARKUS_PREFIX + ".");
        assertThat(capabilityName).doesNotContain("..");
    }

    private static String getString(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            fail(e);
            return "";
        }
    }
}
