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
        CompletionItem match = matches.get(0);
        assertEquals(expected, match);
    }

    public static CompletionItem c(String label, CompletionItemType type) {
        String finalLabel = label;
        int selectionStart = label.indexOf("|");
        int selectionEnd = -1;
        if (selectionStart != -1) {
            selectionEnd = label.indexOf("|", selectionStart + 1);
            if (selectionEnd != -1) {
                finalLabel = label.substring(0, selectionStart)
                        + label.substring(selectionStart + 1, selectionEnd)
                        + label.substring(selectionEnd + 1, label.length());

            }
        }

        CompletionItem item = new CompletionItem();
        item.setLabel(finalLabel);
        item.setType(type);
        if (selectionEnd != -1) {
            item.setSelectionStart(selectionStart);
            item.setSelectionLength(selectionEnd - selectionStart - 1);
        }
        return item;
    }

}
