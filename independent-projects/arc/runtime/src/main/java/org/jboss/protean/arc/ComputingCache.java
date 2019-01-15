/*
 * Copyright 2018 Red Hat, Inc.
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

package org.jboss.protean.arc;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Computing cache backed by a {@link ConcurrentHashMap} which intentionally does not use {@link Map#computeIfAbsent(Object, Function)} and is reentrant.
 * Derived from {@code org.jboss.weld.util.cache.ReentrantMapBackedComputingCache}.
 *
 * @param <K>
 * @param <V>
 */
public class ComputingCache<K, V> {

    private final ConcurrentMap<K, V> map;

    private final Function<K, V> function;

    public ComputingCache(Function<K, V> computingFunction) {
        this.map = new ConcurrentHashMap<>();
        this.function = computingFunction;
    }

    public V getValue(K key) {
        V value = map.get(key);
        if (value == null) {
            value = function.apply(key);
            V previous = map.putIfAbsent(key, value);
            if (previous != null) {
                value = previous;
            }
        }
        return value;
    }

    public V getValueIfPresent(K key) {
        return map.get(key);
    }

    public V remove(K key) {
        return map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    public void forEachValue(Consumer<? super V> action) {
        Objects.requireNonNull(action);
        for (V value : map.values()) {
            action.accept(value);
        }
    }

    public void forEachEntry(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<K, V> entry : map.entrySet()) {
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

}
