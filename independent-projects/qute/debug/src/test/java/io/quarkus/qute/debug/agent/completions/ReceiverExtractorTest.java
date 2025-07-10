package io.quarkus.qute.debug.agent.completions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ReceiverExtractorTest {

    @Test
    void testSimpleIdentifier() {
        assertEquals("items", extract("items.<caret>size"));
        assertEquals("items", extract("items.s<caret>ize"));
        assertEquals("items.size", extract("items.size.in<caret>dex"));
    }

    @Test
    void testLogicalExpression() {
        assertEquals("items", extract("items.size > 10 && items.<caret>"));
        assertEquals("items", extract("items.size > 10 && items.s<caret>ize"));
    }

    @Test
    void testNestedMethodCall() {
        assertEquals("items", extract("items.get(items.<caret>size)"));
        assertEquals("items", extract("items.get(items.si<caret>ze)"));
    }

    @Test
    void testMultipleDots() {
        assertEquals("a.b.c", extract("a.b.c.d<caret>"));
        assertEquals("foo.bar", extract("map.get(foo.bar.b<caret>az)"));
    }

    @Test
    void testEmptyOrInvalidCases() {
        assertNull(extract("<caret>"));
        assertNull(extract("map.get(<caret>)"));
    }

    @Test
    void testArrayIndexAccess() {
        assertEquals("items[0]", extract("items[0].<caret>size"));
        assertEquals("map['key']", extract("map['key'].val<caret>ue"));
    }

    @Test
    void testMethodCallCaret() {
        assertEquals("items.get()", extract("items.get().<caret>size"));
        assertEquals("items", extract("items.get(items.<caret>size)"));
    }

    @Test
    void testChainedAccess() {
        assertEquals("a.b.c", extract("a.b.c.d<caret>"));
    }

    @Test
    void testInvalidCases() {
        assertNull(extract("<caret>"));
        assertNull(extract("map.get(<caret>)"));
    }

    @Test
    void testDotWithNoIdentifier() {
        assertEquals("items", extract("items.<caret>"));
        assertEquals("foo.bar", extract("foo.bar.<caret>"));
        assertEquals("items", extract("items.size > 10 && items.<caret>"));
    }

    private String extract(String exprWithCaret) {
        int caret = exprWithCaret.indexOf("<caret>");
        assertTrue(caret >= 0, "Missing <caret> marker in test expression");
        String expr = exprWithCaret.replace("<caret>", "");
        return CompletionSupport.extractReceiverBeforeCaret(expr, caret - 1);
    }
}
