package io.quarkus.arc.processor;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An unmodifiable live view that concatenates two collections without copying elements.
 * <p>
 * Changes to the underlying collections are immediately reflected in this view.
 *
 * @param <E> the element type
 */
class ConcatCollectionView<E> extends AbstractCollection<E> {

    private final Collection<E> first;
    private final Collection<E> second;

    ConcatCollectionView(Collection<E> first, Collection<E> second) {
        this.first = Objects.requireNonNull(first);
        this.second = Objects.requireNonNull(second);
    }

    @Override
    public int size() {
        return first.size() + second.size();
    }

    @Override
    public boolean isEmpty() {
        return first.isEmpty() && second.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return first.contains(o) || second.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            final Iterator<E> firstIt = first.iterator();
            final Iterator<E> secondIt = second.iterator();

            @Override
            public boolean hasNext() {
                return firstIt.hasNext() || secondIt.hasNext();
            }

            @Override
            public E next() {
                if (firstIt.hasNext()) {
                    return firstIt.next();
                }
                if (secondIt.hasNext()) {
                    return secondIt.next();
                }
                throw new NoSuchElementException();
            }
        };
    }

}
