package io.quarkus.test.junit5.virtual.internal;

import static io.quarkus.test.junit5.virtual.internal.JUnitEngine.runTestAndAssertFailure;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.junit5.virtual.internal.ignore.LoomUnitExampleOnMethodTest;
import io.quarkus.test.junit5.virtual.internal.ignore.LoomUnitExampleShouldNotPinOnClassTest;
import io.quarkus.test.junit5.virtual.internal.ignore.LoomUnitExampleShouldNotPinOnSuperClassTest;
import io.quarkus.test.junit5.virtual.internal.ignore.LoomUnitExampleShouldPinOnSuperClassTest;

public class ShouldPinTest {

    @ParameterizedTest
    @MethodSource
    @EnabledForJreRange(min = JRE.JAVA_21)
    void testShouldPinButNoPinEventDetected(Class<?> clazz, String methodName) {
        runTestAndAssertFailure(clazz, methodName, "was expected to pin the carrier thread, it didn't");
    }

    public static Stream<Arguments> testShouldPinButNoPinEventDetected() {
        return Stream.of(
                arguments(LoomUnitExampleOnMethodTest.class, "failWhenMethodShouldPinButNoPinDetected"),
                arguments(LoomUnitExampleShouldNotPinOnClassTest.class, "failWhenMethodShouldPinButNoPinDetected"),
                arguments(LoomUnitExampleShouldNotPinOnSuperClassTest.class, "failWhenShouldPinAndNoPinDetected"),
                arguments(LoomUnitExampleShouldPinOnSuperClassTest.class, "failWhenShouldPinAndNoPinDetected"));
    }

}
