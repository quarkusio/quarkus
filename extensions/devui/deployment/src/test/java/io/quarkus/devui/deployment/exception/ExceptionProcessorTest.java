package io.quarkus.devui.deployment.exception;

import static io.quarkus.devui.deployment.exception.ExceptionProcessor.MAX_CAUSE_DEPTH;
import static io.quarkus.devui.deployment.exception.ExceptionProcessor.MAX_CAUSE_MESSAGE_LENGTH;
import static io.quarkus.devui.deployment.exception.ExceptionProcessor.MAX_STACK_TRACE_LINES;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ExceptionProcessorTest {

    @Test
    void simpleException() {
        Map<String, Object> result = populate(new RuntimeException("something broke"));

        assertThat(result)
                .containsEntry("exceptionClass", "java.lang.RuntimeException")
                .containsEntry("message", "something broke");
        assertThat((String) result.get("stackTrace")).contains("RuntimeException: something broke");
        assertThat(result).doesNotContainKey("causeChain");
    }

    @Test
    void causeChain() {
        Exception root = new IllegalArgumentException("bad value");
        Exception mid = new IllegalStateException("invalid state", root);
        Exception top = new RuntimeException("operation failed", mid);

        Map<String, Object> result = populate(top);

        assertThat(result).containsEntry("exceptionClass", "java.lang.RuntimeException");
        String causeChain = (String) result.get("causeChain");
        assertThat(causeChain)
                .startsWith("IllegalStateException: invalid state")
                .contains("IllegalArgumentException: bad value");
    }

    @Test
    void causeChainRespectsMaxDepth() {
        // Build a chain deeper than MAX_CAUSE_DEPTH
        Exception current = new Exception("root");
        for (int i = 0; i < MAX_CAUSE_DEPTH + 3; i++) {
            current = new Exception("level-" + i, current);
        }

        Map<String, Object> result = populate(current);

        String causeChain = (String) result.get("causeChain");
        // Count the separators to determine how many causes are listed
        int causeCount = causeChain.split(" <- ").length;
        assertThat(causeCount).isEqualTo(MAX_CAUSE_DEPTH);
    }

    @Test
    void circularCauseChain() {
        Exception a = new Exception("a");
        Exception b = new Exception("b", a);
        // Create a circular reference: a -> b -> a
        try {
            var causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
            causeField.set(a, b);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not set up circular cause chain", e);
        }

        Map<String, Object> result = populate(a);

        // Should terminate without infinite loop
        assertThat(result).containsKey("causeChain");
        String causeChain = (String) result.get("causeChain");
        assertThat(causeChain).contains("Exception: b");
    }

    @Test
    void suppressedException() {
        Exception primary = new RuntimeException("primary");
        primary.addSuppressed(new IllegalStateException("suppressed-1"));
        primary.addSuppressed(new IllegalArgumentException("suppressed-2"));

        Map<String, Object> result = populate(primary);

        // Suppressed exceptions appear in the full stack trace
        String stackTrace = (String) result.get("stackTrace");
        assertThat(stackTrace)
                .contains("Suppressed: java.lang.IllegalStateException: suppressed-1")
                .contains("Suppressed: java.lang.IllegalArgumentException: suppressed-2");
    }

    @Test
    void stackTraceTruncation() {
        // Create an exception with a very deep stack trace by generating one
        RuntimeException exception = buildDeepException(MAX_STACK_TRACE_LINES + 100);

        Map<String, Object> result = populate(exception);

        String stackTrace = (String) result.get("stackTrace");
        String[] lines = stackTrace.split("\n");
        // Should be truncated to MAX_STACK_TRACE_LINES + 1 (the truncation message)
        assertThat(lines.length).isLessThanOrEqualTo(MAX_STACK_TRACE_LINES + 1);
        assertThat(stackTrace).contains("more lines truncated");
    }

    @Test
    void longCauseMessageTruncated() {
        String longMessage = "x".repeat(MAX_CAUSE_MESSAGE_LENGTH + 200);
        Exception root = new Exception(longMessage);
        Exception top = new RuntimeException("top", root);

        Map<String, Object> result = populate(top);

        String causeChain = (String) result.get("causeChain");
        assertThat(causeChain).contains("...");
        // The cause message portion should not exceed MAX_CAUSE_MESSAGE_LENGTH + "..."
        String causeMessage = causeChain.substring(causeChain.indexOf(": ") + 2);
        assertThat(causeMessage.length()).isLessThanOrEqualTo(MAX_CAUSE_MESSAGE_LENGTH + 3);
    }

    @Test
    void nullMessage() {
        Exception exception = new RuntimeException((String) null);

        Map<String, Object> result = populate(exception);

        assertThat(result)
                .containsEntry("exceptionClass", "java.lang.RuntimeException")
                .containsEntry("message", null);
    }

    @Test
    void causeWithNullMessage() {
        Exception root = new Exception((String) null);
        Exception top = new RuntimeException("top", root);

        Map<String, Object> result = populate(top);

        String causeChain = (String) result.get("causeChain");
        // Should contain the class name but no ": message" part
        assertThat(causeChain).contains("Exception");
        assertThat(causeChain).doesNotContain(": null");
    }

    private static Map<String, Object> populate(Throwable throwable) {
        return populate(throwable, MAX_CAUSE_DEPTH);
    }

    private static Map<String, Object> populate(Throwable throwable, int maxCauseDepth) {
        Map<String, Object> result = new LinkedHashMap<>();
        ExceptionProcessor.populateExceptionDetails(result, throwable, maxCauseDepth);
        return result;
    }

    private static RuntimeException buildDeepException(int targetLines) {
        try {
            return buildDeepExceptionRecursive(targetLines, 0);
        } catch (RuntimeException e) {
            return e;
        }
    }

    private static RuntimeException buildDeepExceptionRecursive(int targetDepth, int current) {
        if (current >= targetDepth) {
            throw new RuntimeException("deep exception");
        }
        return buildDeepExceptionRecursive(targetDepth, current + 1);
    }
}
