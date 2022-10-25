package io.quarkus.qute.runtime.extensions;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import jakarta.enterprise.inject.Vetoed;

import io.quarkus.qute.TemplateExtension;

@Vetoed // Make sure no bean is created from this class
@TemplateExtension
public class CollectionTemplateExtensions {

    static <T> T get(List<T> list, int index) {
        return list.get(index);
    }

    @TemplateExtension(matchRegex = "\\d{1,10}")
    static <T> T getByIndex(List<T> list, String index) {
        return list.get(Integer.parseInt(index));
    }

    static <T> Iterator<T> reversed(List<T> list) {
        ListIterator<T> it = list.listIterator(list.size());
        return new Iterator<T>() {

            @Override
            public boolean hasNext() {
                return it.hasPrevious();
            }

            @Override
            public T next() {
                return it.previous();
            }
        };
    }

    static <T> List<T> take(List<T> list, int n) {
        if (n < 1 || n > list.size()) {
            throw new IndexOutOfBoundsException(n);
        }
        if (list.isEmpty()) {
            return list;
        }
        return list.subList(0, n);
    }

    static <T> List<T> takeLast(List<T> list, int n) {
        if (n < 1 || n > list.size()) {
            throw new IndexOutOfBoundsException(n);
        }
        if (list.isEmpty()) {
            return list;
        }
        return list.subList(list.size() - n, list.size());
    }

    // This extension method has higher priority than ValueResolvers.orEmpty()
    // and makes it possible to validate expressions derived from {list.orEmpty}
    static <T> Collection<T> orEmpty(Collection<T> iterable) {
        return iterable != null ? iterable : Collections.emptyList();
    }

    static <T> T first(List<T> list) {
        if (list.isEmpty()) {
            throw new NoSuchElementException();
        }
        return list.get(0);
    }

    static <T> T last(List<T> list) {
        if (list.isEmpty()) {
            throw new NoSuchElementException();
        }
        return list.get(list.size() - 1);
    }

}
