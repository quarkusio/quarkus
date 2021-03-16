package io.quarkus.it.main;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ParameterizedArrayTestCase {
    @ParameterizedTest
    @NullAndEmptySource
    public void oneDimentionIntArrayArgument(int[] arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void twoDimentionsIntArrayArgument(int[][] arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void threeDimentionsIntArrayArgument(int[][][] arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void oneDimensionStringArrayArgument(String[] arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void twoDimensionsStringArrayArgument(String[][] arg) {
        // the test only verifies that the test method is invoked successfully
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void threeDimensionsStringArrayArgument(String[][][] arg) {
        // the test only verifies that the test method is invoked successfully
    }
}
