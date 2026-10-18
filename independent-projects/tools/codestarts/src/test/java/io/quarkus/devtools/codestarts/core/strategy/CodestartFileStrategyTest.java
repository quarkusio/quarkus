package io.quarkus.devtools.codestarts.core.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

class CodestartFileStrategyTest {

    @Test
    void testFilterStart() {
        final CodestartFileStrategy strategy = new CodestartFileStrategy("*.txt", mock(CodestartFileStrategyHandler.class));
        assertThat(strategy.test("myfile.txt")).isTrue();
        assertThat(strategy.test(null)).isFalse();
        assertThat(strategy.test("foo/bar/myfile.txt")).isTrue();
        assertThat(strategy.test(".txt")).isTrue();
        assertThat(strategy.test("foo/bar/myfile.zip")).isFalse();
        assertThat(strategy.test("")).isFalse();
    }

    @Test
    void testFilterEnd() {
        final CodestartFileStrategy strategy = new CodestartFileStrategy("/foo/bar/*",
                mock(CodestartFileStrategyHandler.class));
        assertThat(strategy.test("/foo/bar/myfile.txt")).isTrue();
        assertThat(strategy.test("/foo/bar/baz/anoter_file")).isTrue();
        assertThat(strategy.test(null)).isFalse();
        assertThat(strategy.test("foo/bar/myfile.txt")).isFalse();
        assertThat(strategy.test("something")).isFalse();
        assertThat(strategy.test("")).isFalse();
    }

    @Test
    void testFilterMiddle() {
        final CodestartFileStrategy strategy = new CodestartFileStrategy("/foo/bar/my*.txt",
                mock(CodestartFileStrategyHandler.class));
        assertThat(strategy.test("/foo/bar/myfile.txt")).isTrue();
        assertThat(strategy.test("/foo/bar/baz/anoter_file")).isFalse();
        assertThat(strategy.test(null)).isFalse();
        assertThat(strategy.test("foo/bar/myfile.txt")).isFalse();
        assertThat(strategy.test("something")).isFalse();
        assertThat(strategy.test("")).isFalse();
    }

    @Test
    void testFilter() {
        final CodestartFileStrategy strategy = new CodestartFileStrategy("/foo/bar/myfile.txt",
                mock(CodestartFileStrategyHandler.class));
        assertThat(strategy.test("/foo/bar/myfile.txt")).isTrue();
        assertThat(strategy.test("/foo/bar/myfile.tx")).isFalse();
        assertThat(strategy.test(null)).isFalse();
        assertThat(strategy.test("foo/bar/myfile.txt")).isFalse();
        assertThat(strategy.test("something")).isFalse();
        assertThat(strategy.test("")).isFalse();
    }
}