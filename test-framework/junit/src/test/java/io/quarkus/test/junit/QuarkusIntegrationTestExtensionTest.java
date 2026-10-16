package io.quarkus.test.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

public class QuarkusIntegrationTestExtensionTest {

    @Test
    public void tailReturnsLastLinesWithHeaderWhenLogExceedsLimit() {
        List<String> lines = IntStream.rangeClosed(1, 120).mapToObj(i -> "line " + i).toList();

        List<String> tail = QuarkusIntegrationTestExtension.applicationLogTail(lines, 50, "quarkus.log");

        assertThat(tail).hasSize(51); // 1 header + 50 lines
        assertThat(tail.get(0)).isEqualTo("Last 50 line(s) of quarkus.log:");
        assertThat(tail.get(1)).isEqualTo("    line 71");
        assertThat(tail.get(50)).isEqualTo("    line 120");
    }

    @Test
    public void tailReturnsAllLinesWhenLogIsSmallerThanLimit() {
        List<String> lines = List.of("first", "second", "third");

        List<String> tail = QuarkusIntegrationTestExtension.applicationLogTail(lines, 50, "quarkus.log");

        assertThat(tail).containsExactly(
                "Last 3 line(s) of quarkus.log:",
                "    first",
                "    second",
                "    third");
    }

    @Test
    public void tailReturnsEmptyForEmptyLog() {
        assertThat(QuarkusIntegrationTestExtension.applicationLogTail(List.of(), 50, "quarkus.log")).isEmpty();
    }

    @Test
    public void clearBootFailureClearsStaticBootFailureState() throws Exception {
        Field failedBoot = QuarkusIntegrationTestExtension.class.getDeclaredField("failedBoot");
        failedBoot.setAccessible(true);
        failedBoot.setBoolean(null, true);
        Field firstException = QuarkusIntegrationTestExtension.class.getDeclaredField("firstException");
        firstException.setAccessible(true);
        firstException.set(null, new RuntimeException("boom"));

        QuarkusIntegrationTestExtension.clearBootFailure();

        assertThat(failedBoot.getBoolean(null)).isFalse();
        assertThat(firstException.get(null)).isNull();
    }
}
