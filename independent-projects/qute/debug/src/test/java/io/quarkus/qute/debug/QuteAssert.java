package io.quarkus.qute.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionItemType;

public class QuteAssert {

    public static void assertCompletion(CompletionItem[] expected, CompletionItem[] actual) {
        assertCompletion(expected, actual, null);
    }

    public static void assertCompletion(CompletionItem[] expected, CompletionItem[] actual, Integer expectedCount) {
        for (int i = 0; i < expected.length; i++) {
            assertCompletion(actual, expected[i], expectedCount);
        }
    }

    public static void assertCompletion(CompletionItem[] actual, CompletionItem expected, Integer expectedCount) {
        List<CompletionItem> matches = Stream.of(actual).filter(completion -> {
            return expected.getLabel().equals(completion.getLabel());
        }).collect(Collectors.toList());

        if (expectedCount != null) {
            assertTrue(matches.size() >= 1, () -> {
                return expected.getLabel() + " should only exist once: Actual: "
                        + Stream.of(actual).map(c -> c.getLabel()).collect(Collectors.joining(","));
            });
        } else {
            assertEquals(1, matches.size(), () -> {
                return expected.getLabel() + " should only exist once: Actual: "
                        + Stream.of(actual).map(c -> c.getLabel()).collect(Collectors.joining(","));
            });
        }
    }

    public static CompletionItem c(String label, CompletionItemType type) {
        CompletionItem item = new CompletionItem();
        item.setLabel(label);
        item.setType(type);
        return item;
    }

}
