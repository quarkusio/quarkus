package io.quarkus.hibernate.orm.deployment;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Set;

import org.jboss.jandex.DotName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ConstantsTest {
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
