package io.quarkus.rest.server.servlet.runtime;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * Single shot wrapper for an enum that allows it to be used in for-each loops
 * 
 * @param <T>
 */
public class EnumerationIterable<T> implements Iterable<T>, Iterator<T> {
    private final Enumeration<T> enumeration;

    public EnumerationIterable(Enumeration<T> enumeration) {
        this.enumeration = enumeration;
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return enumeration.hasMoreElements();
    }

    @Override
    public T next() {
        return enumeration.nextElement();
    }
}
