package io.quarkus.qute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class ImmutableListTest {

    @Test
    public void testListOfTwo() {
        List<String> list = ImmutableList.of("foo", "BAR");
        assertEquals(2, list.size());
        assertEquals("foo", list.get(0));
        assertEquals("BAR", list.get(1));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> list.remove(0));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> list.add("baz"));

        Iterator<String> it = list.iterator();
        assertTrue(it.hasNext());
        assertEquals("foo", it.next());
        assertEquals("BAR", it.next());
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> it.next());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> it.remove());
        ListIterator<String> listIt = list.listIterator();
        assertFalse(listIt.hasPrevious());
        assertTrue(listIt.hasNext());
        assertEquals("foo", listIt.next());
        assertEquals("BAR", listIt.next());
        assertEquals(1, listIt.previousIndex());
        assertEquals("BAR", listIt.previous());
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> it.next());
        assertThatExceptionOfType(IndexOutOfBoundsException.class)
                .isThrownBy(() -> list.get(5));
    }

}
