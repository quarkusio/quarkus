package io.quarkus.qute.runtime.extensions;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.enterprise.inject.Vetoed;

import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateExtension;

@Vetoed // Make sure no bean is created from this class
@TemplateExtension
public class CollectionTemplateExtensions {

    static <T> T get(List<T> list, int index) {
        return list.get(index);
    }

    @SuppressWarnings("unchecked")
    @TemplateExtension(matchRegex = "\\d{1,10}")
    static <T> T getByIndex(List<T> list, String index) {
        int idx = Integer.parseInt(index);
        if (idx >= list.size()) {
            // Be consistent with property resolvers
            return (T) Results.NotFound.from(index);
        }
        return list.get(idx);
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

}
