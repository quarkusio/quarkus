package io.quarkus.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        final int size;
        if (list instanceof ArrayList<T> && (size = list.size()) <= 2) {
            switch (size) {
                case 0:
                    return List.of();
                case 1:
                    return List.of(list.get(0));
                case 2:
                    return List.of(list.get(0), list.get(1));
            }
        }
        return List.copyOf(list);
    }

    /**
     *
     * @param elements
     * @return an immutable list of the given elements
     */
    @SafeVarargs
    public static <T> List<T> of(T... elements) {
        return switch (elements.length) {
            case 0 -> List.of();
            case 1 -> List.of(elements[0]);
            case 2 -> List.of(elements[0], elements[1]);
            default -> List.of(elements);
        };
    }

    /**
     *
     * @param <E>
     * @param element
     * @return an immutable list
     */
    public static <E> List<E> of(E element) {
        return List.of(element);
    }

    /**
     *
     * @param <E>
     * @param e1
     * @param e2
     * @return an immutable list
     */
    public static <E> List<E> of(E e1, E e2) {
        return List.of(e1, e2);
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

}
