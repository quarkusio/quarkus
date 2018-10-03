/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logmanager;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import java.util.logging.Handler;

/**
 * Utility for snapshot/copy-on-write arrays.  To use these methods, two things are required: an immutable array
 * stored on a volatile field, and an instance of
 * {@link java.util.concurrent.atomic.AtomicReferenceFieldUpdater AtomicReferenceFieldUpdater}
 * which corresponds to that field.  Some of these methods perform multi-step operations; if the array field value is
 * changed in the middle of such an operation, the operation is retried.  To avoid spinning, in some situations it
 * may be advisable to hold a write lock to prevent multiple concurrent updates.
 *
 * @param <T> the type which contains the target field
 * @param <V> the array value type
 */
final class AtomicArray<T, V> {

    private final AtomicReferenceFieldUpdater<T, V[]> updater;
    private final Class<V> componentType;
    private final V[] emptyArray;

    /**
     * Construct an instance.
     *
     * @param updater the field updater
     * @param componentType the component class
     */
    public AtomicArray(AtomicReferenceFieldUpdater<T, V[]> updater, Class<V> componentType) {
        this.updater = updater;
        this.componentType = componentType;
        emptyArray = newInstance(componentType, 0);
    }

    /**
     * Convenience method to create an instance.
     *
     * @param updater the field updater
     * @param componentType the component class
     * @param <T> the type which contains the target field
     * @param <V> the array value type
     * @return the new instance
     */
    public static <T, V> AtomicArray<T, V> create(AtomicReferenceFieldUpdater<T, V[]> updater, Class<V> componentType) {
        return new AtomicArray<T,V>(updater, componentType);
    }

    /**
     * Convenience method to set the field value to the empty array.  Empty array instances are shared.
     *
     * @param instance the instance holding the field
     */
    public void clear(T instance) {
        updater.set(instance, emptyArray);
    }

    /**
     * Update the value of this array.
     *
     * @param instance the instance holding the field
     * @param value the new value
     */
    public void set(T instance, V[] value) {
        updater.set(instance, value);
    }

    /**
     * Atomically get and update the value of this array.
     *
     * @param instance the instance holding the field
     * @param value the new value
     */
    public V[] getAndSet(T instance, V[] value) {
        return updater.getAndSet(instance, value);
    }

    @SuppressWarnings({ "unchecked" })
    private static <V> V[] copyOf(final Class<V> componentType, V[] old, int newLen) {
        final V[] target = newInstance(componentType, newLen);
        System.arraycopy(old, 0, target, 0, Math.min(old.length, newLen));
        return target;
    }

    /**
     * Atomically replace the array with a new array which is one element longer, and which includes the given value.
     *
     * @param instance the instance holding the field
     * @param value the updated value
     */
    public void add(T instance, V value) {
        final AtomicReferenceFieldUpdater<T, V[]> updater = this.updater;
        for (;;) {
            final V[] oldVal = updater.get(instance);
            final int oldLen = oldVal.length;
            final V[] newVal = copyOf(componentType, oldVal, oldLen + 1);
            newVal[oldLen] = value;
            if (updater.compareAndSet(instance, oldVal, newVal)) {
                return;
            }
        }
    }

    /**
     * Atomically replace the array with a new array which is one element longer, and which includes the given value,
     * if the value is not already present within the array.  This method does a linear search for the target value.
     *
     * @param instance the instance holding the field
     * @param value the updated value
     * @param identity {@code true} if comparisons should be done using reference identity, or {@code false} to use the {@code equals()} method
     * @return {@code true} if the value was added, or {@code false} if it was already present
     */
    public boolean addIfAbsent(T instance, V value, boolean identity) {
        final AtomicReferenceFieldUpdater<T, V[]> updater = this.updater;
        for (;;) {
            final V[] oldVal = updater.get(instance);
            final int oldLen = oldVal.length;
            if (identity || value == null) {
                for (int i = 0; i < oldLen; i++) {
                    if (oldVal[i] == value) {
                        return false;
                    }
                }
            } else {
                for (int i = 0; i < oldLen; i++) {
                    if (value.equals(oldVal[i])) {
                        return false;
                    }
                }
            }
            final V[] newVal = copyOf(componentType, oldVal, oldLen + 1);
            newVal[oldLen] = value;
            if (updater.compareAndSet(instance, oldVal, newVal)) {
                return true;
            }
        }
    }

    /**
     * Atomically replace the array with a new array which does not include the first occurrance of the given value, if
     * the value is present in the array.
     *
     * @param instance the instance holding the field
     * @param value the updated value
     * @param identity {@code true} if comparisons should be done using reference identity, or {@code false} to use the {@code equals()} method
     * @return {@code true} if the value was removed, or {@code false} if it was not present
     */
    public boolean remove(T instance, V value, boolean identity) {
        final AtomicReferenceFieldUpdater<T, V[]> updater = this.updater;
        for (;;) {
            final V[] oldVal = updater.get(instance);
            final int oldLen = oldVal.length;
            if (oldLen == 0) {
                return false;
            } else {
                int index = -1;
                if (identity || value == null) {
                    for (int i = 0; i < oldLen; i++) {
                        if (oldVal[i] == value) {
                            index = i;
                            break;
                        }
                    }
                } else {
                    for (int i = 0; i < oldLen; i++) {
                        if (value.equals(oldVal[i])) {
                            index = i;
                            break;
                        }
                    }
                }
                if (index == -1) {
                    return false;
                }
                final V[] newVal = newInstance(componentType, oldLen - 1);
                System.arraycopy(oldVal, 0, newVal, 0, index);
                System.arraycopy(oldVal, index + 1, newVal, index, oldLen - index - 1);
                if (updater.compareAndSet(instance, oldVal, newVal)) {
                    return true;
                }
            }
        }
    }

    /**
     * Atomically replace the array with a new array which does not include any occurrances of the given value, if
     * the value is present in the array.
     *
     * @param instance the instance holding the field
     * @param value the updated value
     * @param identity {@code true} if comparisons should be done using reference identity, or {@code false} to use the {@code equals()} method
     * @return the number of values removed
     */
    public int removeAll(T instance, V value, boolean identity) {
        final AtomicReferenceFieldUpdater<T, V[]> updater = this.updater;
        for (;;) {
            final V[] oldVal = updater.get(instance);
            final int oldLen = oldVal.length;
            if (oldLen == 0) {
                return 0;
            } else {
                final boolean[] removeSlots = new boolean[oldLen];
                int removeCount = 0;
                if (identity || value == null) {
                    for (int i = 0; i < oldLen; i++) {
                        if (oldVal[i] == value) {
                            removeSlots[i] = true;
                            removeCount++;
                        }
                    }
                } else {
                    for (int i = 0; i < oldLen; i++) {
                        if (value.equals(oldVal[i])) {
                            removeSlots[i] = true;
                            removeCount++;
                        }
                    }
                }
                if (removeCount == 0) {
                    return 0;
                }
                final int newLen = oldLen - removeCount;
                final V[] newVal;
                if (newLen == 0) {
                    newVal = emptyArray;
                } else {
                    newVal = newInstance(componentType, newLen);
                    for (int i = 0, j = 0; i < oldLen; i ++) {
                        if (! removeSlots[i]) {
                            newVal[j++] = oldVal[i];
                        }
                    }
                }
                if (updater.compareAndSet(instance, oldVal, newVal)) {
                    return removeCount;
                }
            }
        }
    }

    /**
     * Add a value to a sorted array.  Does not check for duplicates.
     *
     * @param instance the instance holding the field
     * @param value the value to add
     * @param comparator a comparator, or {@code null} to use natural ordering
     */
    public void add(T instance, V value, Comparator<? super V> comparator) {
        final AtomicReferenceFieldUpdater<T, V[]> updater = this.updater;
        for (;;) {
            final V[] oldVal = updater.get(instance);
            final int oldLen = oldVal.length;
            final int pos = insertionPoint(Arrays.binarySearch(oldVal, value, comparator));
            final V[] newVal = newInstance(componentType, oldLen + 1);
            System.arraycopy(oldVal, 0, newVal, 0, pos);
            newVal[pos] = value;
            System.arraycopy(oldVal, pos, newVal, pos + 1, oldLen - pos);
            if (updater.compareAndSet(instance, oldVal, newVal)) {
                return;
            }
        }
    }

    /**
     * Add a value to a sorted array if it is not already present.  Does not check for duplicates.
     *
     * @param instance the instance holding the field
     * @param value the value to add
     * @param comparator a comparator, or {@code null} to use natural ordering
     */
    public boolean addIfAbsent(T instance, V value, Comparator<? super V> comparator) {
        final AtomicReferenceFieldUpdater<T, V[]> updater = this.updater;
        for (;;) {
            final V[] oldVal = updater.get(instance);
            final int oldLen = oldVal.length;
            final int pos = Arrays.binarySearch(oldVal, value, comparator);
            if (pos < 0) {
                return false;
            }
            final V[] newVal = newInstance(componentType, oldLen + 1);
            System.arraycopy(oldVal, 0, newVal, 0, pos);
            newVal[pos] = value;
            System.arraycopy(oldVal, pos, newVal, pos + 1, oldLen - pos);
            if (updater.compareAndSet(instance, oldVal, newVal)) {
                return true;
            }
        }
    }

    /**
     * Remove a value to a sorted array.  Does not check for duplicates.  If there are multiple occurrances of a value,
     * there is no guarantee as to which one is removed.
     *
     * @param instance the instance holding the field
     * @param value the value to remove
     * @param comparator a comparator, or {@code null} to use natural ordering
     */
    public boolean remove(T instance, V value, Comparator<? super V> comparator) {
        final AtomicReferenceFieldUpdater<T, V[]> updater = this.updater;
        for (;;) {
            final V[] oldVal = updater.get(instance);
            final int oldLen = oldVal.length;
            if (oldLen == 0) {
                return false;
            } else {
                final int pos = Arrays.binarySearch(oldVal, value, comparator);
                if (pos < 0) {
                    return false;
                }
                final V[] newVal = newInstance(componentType, oldLen - 1);
                System.arraycopy(oldVal, 0, newVal, 0, pos);
                System.arraycopy(oldVal, pos + 1, newVal, pos, oldLen - pos - 1);
                if (updater.compareAndSet(instance, oldVal, newVal)) {
                    return true;
                }
            }
        }
    }

    /**
     * Sort an array.
     *
     * @param instance the instance holding the field
     * @param comparator a comparator, or {@code null} to use natural ordering
     */
    public void sort(T instance, Comparator<? super V> comparator) {
        final AtomicReferenceFieldUpdater<T, V[]> updater = this.updater;
        for (;;) {
            final V[] oldVal = updater.get(instance);
            if (oldVal.length == 0) {
                return;
            }
            final V[] newVal = oldVal.clone();
            Arrays.sort(newVal, comparator);
            if (updater.compareAndSet(instance, oldVal, newVal)) {
                return;
            }
        }
    }

    private static int insertionPoint(int searchResult) {
        return searchResult > 0 ? searchResult : - (searchResult + 1);
    }

    @SuppressWarnings({ "unchecked" })
    private static <V> V[] newInstance(Class<V> componentType, int length) {
        if (componentType == Handler.class) {
            return (V[]) new Handler[length];
        } else if (componentType == Object.class) {
            return (V[]) new Object[length];
        } else {
            return (V[]) Array.newInstance(componentType, length);
        }
    }

    /**
     * Compare and set the array.
     *
     * @param instance the instance holding the field
     * @param expect the expected value
     * @param update the update value
     * @return {@code true} if the value was updated or {@code false} if the expected value did not match
     */
    public boolean compareAndSet(final T instance, final V[] expect, final V[] update) {
        return updater.compareAndSet(instance, expect, update);
    }
}
