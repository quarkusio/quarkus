package io.quarkus.runtime.util;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Transform to "old school" Enumeration from Iterator/Spliterator/Stream
 */
public class EnumerationUtil {
    public static <T> Enumeration<T> from(Iterator<T> iterator) {
        Objects.requireNonNull(iterator);

        return new Enumeration<T>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public T nextElement() {
                return iterator.next();
            }
        };
    }

    public static <T> Enumeration<T> from(Spliterator<T> spliterator) {
        Objects.requireNonNull(spliterator);

        class Adapter implements Enumeration<T>, Consumer<T> {
            boolean valueReady;
            T nextElement;

            public void accept(T t) {
                this.valueReady = true;
                this.nextElement = t;
            }

            public boolean hasMoreElements() {
                if (!this.valueReady) {
                    spliterator.tryAdvance(this);
                }

                return this.valueReady;
            }

            public T nextElement() {
                if (!this.valueReady && !this.hasMoreElements()) {
                    throw new NoSuchElementException();
                } else {
                    this.valueReady = false;
                    T t = this.nextElement;
                    this.nextElement = null;
                    return t;
                }
            }
        }

        return new Adapter();
    }

    public static <T> Enumeration<T> from(Stream<T> stream) {
        return from(stream.spliterator());
    }
}
