package io.quarkus.it.main;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ParameterizedPrimitiveTestCase {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void booleanArgument(boolean arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @ValueSource(bytes = { 0, 1 })
    public void byteArgument(byte arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @ValueSource(chars = { '0', '1' })
    public void charArgument(char arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @ValueSource(doubles = { 0.0, 1.0 })
    public void doubleArgument(double arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @ValueSource(floats = { 0.0F, 1.0F })
    public void floatArgument(float arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1 })
    public void intArgument(int arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, 1L })
    public void longArgument(long arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @ValueSource(shorts = { 0, 1 })
    public void shortArgument(short arg) {
        // the test only verifies that the test method is invoked successfully
    }
}
