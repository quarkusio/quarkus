/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class MapTestUtils {

    /**
     * Compares the two maps have the same keys and same values in any order.
     *
     * @param m1  the first map used to compare the keys and values
     * @param m2  the second map used to compare the keys and values
     * @param <K> the key type
     * @param <V> the value type
     */
    @SuppressWarnings("WeakerAccess")
    public static <K, V> void compareMaps(final Map<K, V> m1, final Map<K, V> m2) {
        String failureMessage = String.format("Keys did not match%n%s%n%s%n", m1.keySet(), m2.keySet());
        Assert.assertTrue(failureMessage, m1.keySet().containsAll(m2.keySet()));
        Assert.assertTrue(failureMessage, m2.keySet().containsAll(m1.keySet()));

        // At this point we know that all the keys match
        for (K key : m1.keySet()) {
            final V value1 = m1.get(key);
            final V value2 = m2.get(key);
            Assert.assertEquals(
                    String.format("Value %s from the first map does not match value %s from the second map with key %s.", value1, value2, key),
                    value1, value2);
        }
    }

    /**
     * A helper to easily build maps. The resulting map is immutable and the order is predictable with the
     * {@link #add(Object, Object)} order.
     *
     * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
     */
    public static class MapBuilder<K, V> {
        private final Map<K, V> result;

        private MapBuilder(final Map<K, V> result) {
            this.result = result;
        }

        public static <K, V> MapBuilder<K, V> create() {
            return new MapBuilder<>(new LinkedHashMap<K, V>());
        }

        public MapBuilder<K, V> add(final K key, final V value) {
            result.put(key, value);
            return this;
        }

        public Map<K, V> build() {
            return Collections.unmodifiableMap(result);
        }
    }
}
