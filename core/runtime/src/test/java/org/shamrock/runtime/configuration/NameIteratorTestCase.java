package org.shamrock.runtime.configuration;

import static org.junit.Assert.*;

import java.util.NoSuchElementException;

import org.jboss.shamrock.runtime.configuration.NameIterator;
import org.junit.Test;

/**
 */
public class NameIteratorTestCase {

    @Test
    public void testConstruction() {
        final String string = "simpleName";
        final int length = string.length();

        NameIterator ni = new NameIterator(string);
        assertEquals(-1, ni.getPosition());
        assertEquals(length, ni.getNextEnd());
        try {
            ni.getPreviousStart();
            fail("Expected exception");
        } catch (NoSuchElementException ok) {}
        assertEquals(string, ni.getNextSegment());
        try {
            ni.getPreviousSegment();
            fail("Expected exception");
        } catch (NoSuchElementException ok) {}

        ni = new NameIterator(string, true);
        assertEquals(length, ni.getPosition());
        assertEquals(0, ni.getPreviousStart());
        try {
            ni.getNextEnd();
            fail("Expected exception");
        } catch (NoSuchElementException ok) {}
    }

    @SafeVarargs
    private static <T> T[] array(T... objs) {
        return objs;
    }

    private static String join(String[] str) {
        final int length = str.length;
        if (length == 0) return "";
        StringBuilder b = new StringBuilder(40);
        b.append(str[0]);
        int i = 1;
        while (i < length) {
            b.append('.').append(str[i++]);
        }
        return b.toString();
    }

    @Test
    public void testIteration() {
        String[] items = array("foo", "banana", "bar", "apple", "baz", "grape");
        final int length = items.length;
        String joined = join(items);

        NameIterator ni = new NameIterator(joined);
        assertTrue(ni.hasNext());
        assertEquals(items[0], ni.getNextSegment());
        assertTrue(ni.nextSegmentEquals(items[0]));
        assertFalse(ni.hasPrevious());

        for (int i = 1; i < length; i ++) {
            ni.next();
            assertTrue(ni.hasNext());
            assertEquals(items[i], ni.getNextSegment());
            assertTrue(ni.nextSegmentEquals(items[i]));
            assertTrue(ni.hasPrevious());
            assertEquals(items[i - 1], ni.getPreviousSegment());
            assertTrue(ni.previousSegmentEquals(items[i - 1]));
        }

        ni.next();
        assertFalse(ni.hasNext());
        assertTrue(ni.hasPrevious());
        assertEquals(items[length - 1], ni.getPreviousSegment());
        assertTrue(ni.previousSegmentEquals(items[length - 1]));
        ni.previous();

        for (int i = length - 1; i >= 1; i --) {
            assertTrue(ni.hasNext());
            assertEquals(items[i], ni.getNextSegment());
            assertTrue(ni.nextSegmentEquals(items[i]));
            assertTrue(ni.hasPrevious());
            assertEquals(items[i - 1], ni.getPreviousSegment());
            assertTrue(ni.previousSegmentEquals(items[i - 1]));
            ni.previous();
        }

        assertEquals(-1, ni.getPosition());
        assertTrue(ni.hasNext());
        assertEquals(items[0], ni.getNextSegment());
        assertTrue(ni.nextSegmentEquals(items[0]));
        assertFalse(ni.hasPrevious());
    }

    @Test
    public void testIterationWithQuoted() {
        String[] rawItems = array("foo", "\"banana\"", "\"bar.bar\"", "ap\"\"ple", "b\\\"az", "g\"r\"a\"p\"e");
        String[] items = array("foo", "banana", "bar.bar", "apple", "b\"az", "grape");
        final int length = rawItems.length;
        String joined = join(rawItems);

        NameIterator ni = new NameIterator(joined);
        assertTrue(ni.hasNext());
        assertEquals(items[0], ni.getNextSegment());
        assertTrue(ni.nextSegmentEquals(items[0]));
        assertFalse(ni.hasPrevious());

        for (int i = 1; i < length; i ++) {
            ni.next();
            assertTrue(ni.hasNext());
            assertEquals(items[i], ni.getNextSegment());
            assertTrue(ni.nextSegmentEquals(items[i]));
            assertTrue(ni.hasPrevious());
            assertEquals(items[i - 1], ni.getPreviousSegment());
            assertTrue(ni.previousSegmentEquals(items[i - 1]));
        }

        ni.next();
        assertFalse(ni.hasNext());
        assertTrue(ni.hasPrevious());
        assertEquals(items[length - 1], ni.getPreviousSegment());
        assertTrue(ni.previousSegmentEquals(items[length - 1]));
        ni.previous();

        for (int i = length - 1; i >= 1; i --) {
            assertTrue(ni.hasNext());
            assertEquals(items[i], ni.getNextSegment());
            assertTrue(ni.nextSegmentEquals(items[i]));
            assertTrue(ni.hasPrevious());
            assertEquals(items[i - 1], ni.getPreviousSegment());
            assertTrue(ni.previousSegmentEquals(items[i - 1]));
            ni.previous();
        }

        assertEquals(-1, ni.getPosition());
        assertTrue(ni.hasNext());
        assertEquals(items[0], ni.getNextSegment());
        assertTrue(ni.nextSegmentEquals(items[0]));
        assertFalse(ni.hasPrevious());
    }
}
