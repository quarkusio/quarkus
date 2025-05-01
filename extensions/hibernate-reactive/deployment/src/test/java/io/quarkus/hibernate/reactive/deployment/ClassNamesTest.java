package io.quarkus.hibernate.reactive.deployment;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test that class name constants point to actual classes and stay up-to-date.
 */
public class ClassNamesTest {

    @BeforeAll
    public static void index() throws IOException {
    }

    @ParameterizedTest
    @MethodSource("provideConstantsToTest")
    void testClassNameRefersToExistingClass(DotName constant) {
        assertThatCode(() -> getClass().getClassLoader().loadClass(constant.toString()))
                .doesNotThrowAnyException();
    }

    private static Set<DotName> provideConstantsToTest() {
        return ClassNames.CREATED_CONSTANTS;
    }
}
