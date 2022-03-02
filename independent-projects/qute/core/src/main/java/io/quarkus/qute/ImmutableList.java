package io.quarkus.qute;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * Immutable lists.
 */
public final class ImmutableList {

    private ImmutableList() {
    }

    /**
     *
     * @param list
     * @return an immutable copy of the given list
     */
    public static <T> List<T> copyOf(List<T> list) {
        if (list.isEmpty()) {
            return Collections.emptyList();
        } else if (list.size() == 1) {
            return of(list.get(0));
        } else if (list.size() == 2) {
            return of(list.get(0), list.get(1));
        }
        return new ImmutableArrayList<>(list.toArray());
    }

    /**
     *
     * @param elements
     * @return an immutable list of the given elements
     */
    @SafeVarargs
    public static <T> List<T> of(T... elements) {
        switch (elements.length) {
            case 0:
                return Collections.emptyList();
            case 1:
                return of(elements[0]);
            case 2:
                return of(elements[0], elements[1]);
            default:
                return new ImmutableArrayList<>(elements);
        }
    }

    /**
     *
     * @param <E>
     * @param element
     * @return an immutable list
     */
    public static <E> List<E> of(E element) {
        return Collections.singletonList(element);
    }

    /**
     *
     * @param <E>
     * @param e1
     * @param e2
     * @return an immutable list
     */
    public static <E> List<E> of(E e1, E e2) {
        return new ImmutableList2<>(e1, e2);
    }

    /**
     *
     * @return a builder
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {

        private List<T> elements;

        private Builder() {
            this.elements = new ArrayList<>();
        }

        public Builder<T> add(T element) {
            elements.add(element);
            return this;
        }

        public Builder<T> addAll(Collection<T> elements) {
            this.elements.addAll(elements);
            return this;
        }

        public List<T> build() {
            return copyOf(elements);
        }

    }

    static class ImmutableList2<E> extends AbstractList<E> {

        private final E e0;
        private final E e1;

        ImmutableList2(E e0, E e1) {
            this.e0 = e0;
            this.e1 = e1;
        }

        @Override
        public E get(int index) {
            if (index == 0) {
                return e0;
            } else if (index == 1) {
                return e1;
            }
            throw indexOutOfBound(index, 2);
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            return new Itr();
        }

        @Override
        public ListIterator<E> listIterator() {
            return new Itr();
        }

        private final class Itr implements ListIterator<E> {

            private byte cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public E next() {
                if (cursor == 0) {
                    cursor++;
                    return e0;
                } else if (cursor == 1) {
                    cursor++;
                    return e1;
                }
                throw new NoSuchElementException();
            }

            @Override
            public boolean hasPrevious() {
                return cursor > 0;
            }

            @Override
            public E previous() {
                if (cursor == 2) {
                    cursor--;
                    return e1;
                } else if (cursor == 1) {
                    cursor--;
                    return e0;
                }
                throw new NoSuchElementException();
            }

            @Override
            public int nextIndex() {
                return cursor;
            }

            @Override
            public int previousIndex() {
                return cursor - 1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(E e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(E e) {
                throw new UnsupportedOperationException();
            }
        }

    }

    static class ImmutableArrayList<E> extends AbstractList<E> {

        private final Object[] elements;

        ImmutableArrayList(Object[] elements) {
            this.elements = elements;
        }

        @Override
        public E get(int index) {
            if (index < 0 || index >= elements.length) {
                throw indexOutOfBound(index, size());
            }
            return getInternal(index);
        }

        @SuppressWarnings("unchecked")
        E getInternal(int index) {
            return (E) elements[index];
        }

        @Override
        public int size() {
            return elements.length;
        }

        @Override
        public Iterator<E> iterator() {
            return new Itr(elements.length);
        }

        @Override
        public ListIterator<E> listIterator() {
            return new Itr(elements.length);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            if (fromIndex < 0 || fromIndex > toIndex) {
                throw indexOutOfBound(fromIndex, size());
            }
            if (toIndex > elements.length) {
                throw indexOutOfBound(toIndex, size());
            }
            if (fromIndex == toIndex) {
                return Collections.emptyList();
            }
            return new ImmutableArrayList<>(
                    Arrays.copyOfRange(this.elements, fromIndex, toIndex));
        }

        @Override
        public String toString() {
            return Arrays.toString(elements);
        }

        @Override
        public boolean isEmpty() {
            return elements.length == 0;
        }

        @Override
        public Spliterator<E> spliterator() {
            return Spliterators.spliterator(this, Spliterator.ORDERED
                    | Spliterator.IMMUTABLE | Spliterator.NONNULL);
        }

        private final class Itr implements ListIterator<E> {

            private int cursor;

            private final int size;

            Itr(int size, int position) {
                this.size = size;
                this.cursor = position;
            }

            Itr(int size) {
                this(size, 0);
            }

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public E next() {
                if (hasNext()) {
                    return getInternal(cursor++);
                }
                throw new NoSuchElementException();
            }

            @Override
            public boolean hasPrevious() {
                return cursor > 0;
            }

            @Override
            public E previous() {
                if (hasPrevious()) {
                    return getInternal(--cursor);
                }
                throw new NoSuchElementException();
            }

            @Override
            public int nextIndex() {
                return cursor;
            }

            @Override
            public int previousIndex() {
                return cursor - 1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void set(E e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(E e) {
                throw new UnsupportedOperationException();
            }
        }

    }

    private static IndexOutOfBoundsException indexOutOfBound(int index, int size) {
        return new IndexOutOfBoundsException("Index " + index
                + " is out of bounds, list size: " + size);
    }

}
