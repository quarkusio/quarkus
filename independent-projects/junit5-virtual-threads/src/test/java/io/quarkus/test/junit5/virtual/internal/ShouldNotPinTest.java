package io.quarkus.test.junit5.virtual.internal;

import static io.quarkus.test.junit5.virtual.internal.JUnitEngine.runTestAndAssertFailure;
import static io.quarkus.test.junit5.virtual.internal.JUnitEngine.runTestAndAssertSuccess;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.junit5.virtual.internal.ignore.LoomUnitExampleOnMethodTest;
import io.quarkus.test.junit5.virtual.internal.ignore.LoomUnitExampleShouldNotPinOnClassTest;
import io.quarkus.test.junit5.virtual.internal.ignore.LoomUnitExampleShouldNotPinOnSuperClassTest;
import io.quarkus.test.junit5.virtual.internal.ignore.LoomUnitExampleShouldPinOnSuperClassTest;

public class ShouldNotPinTest {

    @ParameterizedTest
    @MethodSource
    @EnabledForJreRange(min = JRE.JAVA_21)
    void testShouldNotPinButPinEventDetected(Class<?> clazz, String methodName) {
        runTestAndAssertFailure(clazz, methodName, "was expected to NOT pin the carrier thread");
    }

    public static Stream<Arguments> testShouldNotPinButPinEventDetected() {
        return Stream.of(
                arguments(LoomUnitExampleOnMethodTest.class, "failWhenShouldNotPinAndPinDetected"),
                arguments(LoomUnitExampleOnMethodTest.class, "failWhenShouldNotPinAtMostAndTooManyPinDetected"),
                arguments(LoomUnitExampleShouldNotPinOnClassTest.class, "failWhenShouldNotPinAndPinDetected"),
                arguments(LoomUnitExampleShouldNotPinOnSuperClassTest.class, "failWhenShouldNotPinAndPinDetected"),
                arguments(LoomUnitExampleShouldPinOnSuperClassTest.class, "failWhenShouldNotPinAndPinDetected"));
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void shouldNotPinOnMethodOverridesClassAnnotation() {
        runTestAndAssertSuccess(LoomUnitExampleShouldNotPinOnClassTest.class, "overrideClassAnnotation");
        runTestAndAssertSuccess(LoomUnitExampleShouldNotPinOnSuperClassTest.class, "overrideClassAnnotation");
    }

}
