package org.jboss.shamrock.agroal.runtime.graal;

import static java.lang.System.arraycopy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.agroal.pool.util.StampedCopyOnWriteArrayList;

@TargetClass(StampedCopyOnWriteArrayList.class)
final class StampedCopyOnWriteArrayListReplacement<T> {

    @Alias
    private final StampedLock lock;
    @Alias
    private long optimisticStamp;
    @Alias
    private Object[] data;

    // -- //

    @SuppressWarnings("unchecked")
    @Substitute
    public StampedCopyOnWriteArrayListReplacement(Class<? extends T> clazz) {
        this.data = new Object[0];
        lock = new StampedLock();
        optimisticStamp = lock.tryOptimisticRead();
    }

    @Substitute
    public Object[] getUnderlyingArray() {
        Object[] array = data;
        if (lock.validate(optimisticStamp)) {
            return array;
        }

        // Acquiring a read lock does not increment the optimistic stamp
        long stamp = lock.readLock();
        try {
            return data;
        } finally {
            lock.unlockRead(stamp);
        }
    }


    @Substitute
    public T get(int index) {
        return (T) getUnderlyingArray()[index];
    }


    @Substitute
    public int size() {
        return getUnderlyingArray().length;
    }

    // --- //


    @Substitute
    public boolean isEmpty() {
        return size() == 0;
    }


    @Substitute
    public T set(int index, T element) {
        long stamp = lock.writeLock();
        try {
            T old = (T) data[index];
            data[index] = element;
            return old;
        } finally {
            optimisticStamp = lock.tryConvertToOptimisticRead(stamp);
        }
    }


    @Substitute
    public boolean add(T element) {
        long stamp = lock.writeLock();
        try {
            data = UncheckedIterator.copyOf(data, data.length + 1);
            data[data.length - 1] = element;
            return true;
        } finally {
            optimisticStamp = lock.tryConvertToOptimisticRead(stamp);
        }
    }

    @Substitute
    public T removeLast() {
        long stamp = lock.writeLock();
        try {
            T element = (T) data[data.length - 1];
            data = UncheckedIterator.copyOf(data, data.length - 1);
            return element;
        } finally {
            optimisticStamp = lock.tryConvertToOptimisticRead(stamp);
        }
    }


    @Substitute
    public boolean remove(Object element) {
        long stamp = lock.readLock();
        try {
            boolean found = true;

            while (found) {
                Object[] array = data;
                found = false;

                for (int index = array.length - 1; index >= 0; index--) {
                    if (element == array[index]) {
                        found = true;

                        long writeStamp = lock.tryConvertToWriteLock(stamp);
                        if (writeStamp != 0) {
                            stamp = writeStamp;

                            Object[] newData = UncheckedIterator.copyOf(data, data.length - 1);
                            arraycopy(data, index + 1, newData, index, data.length - index - 1);
                            data = newData;
                            return true;
                        } else {
                            break;
                        }
                    }
                }
            }
            return false;
        } finally {
            optimisticStamp = lock.tryConvertToOptimisticRead(stamp);
        }
    }


    @Substitute
    public T remove(int index) {
        long stamp = lock.writeLock();
        try {
            T old = (T) data[index];
            Object[] array = UncheckedIterator.copyOf(data, data.length - 1);
            if (data.length - index - 1 != 0) {
                arraycopy(data, index + 1, array, index, data.length - index - 1);
            }
            data = array;
            return old;
        } finally {
            optimisticStamp = lock.tryConvertToOptimisticRead(stamp);
        }
    }


    @Substitute
    public void clear() {
        long stamp = lock.writeLock();
        try {
            data = UncheckedIterator.copyOf(data, 0);
        } finally {
            optimisticStamp = lock.tryConvertToOptimisticRead(stamp);
        }
    }


    @Substitute
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public Iterator<T> iterator() {
        Object[] array = getUnderlyingArray();
        return new StampedCopyOnWriteArrayListReplacement.UncheckedIterator(array);
    }


    @Substitute
    public boolean addAll(Collection<? extends T> c) {
        long stamp = lock.writeLock();
        try {
            int oldSize = data.length;
            data = UncheckedIterator.copyOf(data, oldSize + c.size());
            for (T element : c) {
                data[oldSize++] = element;
            }
            return true;
        } finally {
            optimisticStamp = lock.tryConvertToOptimisticRead(stamp);
        }
    }

    // --- //


    @Substitute
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public <E> E[] toArray(E[] a) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public void add(int index, T element) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public int indexOf(Object o) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public Stream<T> stream() {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public Stream<T> parallelStream() {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public void forEach(Consumer<? super T> action) {
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            action.accept(it.next());
        }
    }


    @Substitute
    public Spliterator<T> spliterator() {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public boolean removeIf(Predicate<? super T> filter) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public void replaceAll(UnaryOperator<T> operator) {
        throw new UnsupportedOperationException();
    }


    @Substitute
    public void sort(Comparator<? super T> c) {
        throw new UnsupportedOperationException();
    }

    // --- //

    private static final class UncheckedIterator<T> implements Iterator<T> {

        private final int size;

        private final Object[] data;

        private int index = 0;

        public UncheckedIterator(Object[] data) {
            this.data = data;
            this.size = data.length;
        }


        public boolean hasNext() {
            return index < size;
        }


        public T next() {
            if (index < size) {
                return (T) data[index++];
            }
            throw new NoSuchElementException("No more elements in this list");
        }



        public static Object[] copyOf(Object[] original, int newLength) {
            Object[] newArray = new Object[newLength];
            arraycopy(original, 0, newArray, 0, Math.min(newLength, original.length));
            return newArray;
        }
    }

}
