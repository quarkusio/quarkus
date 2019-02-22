package io.quarkus.panache.common;

public class Page {

    public final int index;
    public final int size;

    public Page(int size) {
        this(0, size);
    }

    public Page(int index, int size) {
        if (index < 0)
            throw new IllegalArgumentException("Page index must be > 0 : " + index);
        if (size < 0)
            throw new IllegalArgumentException("Page size must be > 0 : " + size);
        this.index = index;
        this.size = size;
    }

    public static Page of(int index, int size) {
        return new Page(index, size);
    }

    public static Page ofSize(int size) {
        return new Page(size);
    }

    public Page next() {
        return new Page(index + 1, size);
    }

    public Page previous() {
        return index > 0 ? new Page(index - 1, size) : this;
    }

    public Page first() {
        return index > 0 ? new Page(0, size) : this;
    }

    public Page index(int newIndex) {
        return newIndex != index ? new Page(newIndex, size) : this;
    }
}
