package io.quarkus.it.extension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Exercise {@link ParameterizedTest @ParameterizedTest}s.
 *
 * <p>
 * This test will run into <a href="https://github.com/x-stream/xstream/issues/253">x-stream/xstream#253</a> on Java 16 and
 * newer without the necessary {@code --add-opens} options.
 */
@QuarkusTest
public class ParamsTest {
    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void valuesBooleans(boolean ignore) {
    }

    @ParameterizedTest
    @ValueSource(strings = { "true", "false" })
    public void valuesStrings(String ignore) {
    }

    @ParameterizedTest
    @ValueSource(classes = { String.class, TestData.class })
    public void valuesClasses(Class<?> ignore) {
    }

    @ParameterizedTest
    @ValueSource(chars = { 'a', 'b', 'c' })
    public void valuesChars(char ignore) {
    }

    @ParameterizedTest
    @ValueSource(bytes = { (byte) 1, (byte) 2, (byte) 3 })
    public void valuesBytes(byte ignore) {
    }

    @ParameterizedTest
    @MethodSource("testDataStreamList")
    public void methodStreamList(List<String> ignore) {
    }

    static Stream<List<String>> testDataStreamList() {
        return Stream.of(Arrays.asList("a"), Arrays.asList("b"));
    }

    @ParameterizedTest
    @MethodSource("testDataStreamListOf")
    public void methodStreamListOf(List<String> ignore) {
    }

    static Stream<List<String>> testDataStreamListOf() {
        return Stream.of(List.of("a"));
    }

    @ParameterizedTest
    @MethodSource("testDataStreamSetOf")
    public void methodStreamListOf(Set<String> ignore) {
    }

    static Stream<Set<String>> testDataStreamSetOf() {
        return Stream.of(Set.of("a"));
    }

    @ParameterizedTest
    @MethodSource("testDataStreamArrayList")
    public void methodStreamArrayList(List<String> ignore) {
    }

    static Stream<List<String>> testDataStreamArrayList() {
        return Stream.of(Collections.emptyList());
    }

    @ParameterizedTest
    @MethodSource("testDataStream")
    public void methodStream(TestData ignore) {
    }

    static Stream<TestData> testDataStream() {
        return Stream.of(new TestData());
    }

    @ParameterizedTest
    @MethodSource("testDataList")
    public void methodList(TestData ignore) {
    }

    static List<TestData> testDataList() {
        return List.of(new TestData());
    }

    @ParameterizedTest
    @MethodSource("testDataStreamArguments")
    public void methodList(TestData ignore, String ignored) {
    }

    static Stream<Arguments> testDataStreamArguments() {
        return Stream.of(Arguments.of(new TestData(), "foo"));
    }

    @ParameterizedTest
    @MethodSource("testDataListArguments")
    public void methodListArguments(TestData ignore, String ignored) {
    }

    static List<Arguments> testDataListArguments() {
        return Arrays.asList(Arguments.of(new TestData(), "foo"), Arguments.of(new TestData(), "foo"));
    }

    @SuppressWarnings("unused")
    static class TestData {
        final List<String> foo = Arrays.asList("one", "two", "three");
    }
}
