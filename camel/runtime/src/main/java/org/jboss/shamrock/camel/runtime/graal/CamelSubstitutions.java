package org.jboss.shamrock.camel.runtime.graal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.support.ServiceHelper;

class CamelSubstitutions {
}


@Substitute
@TargetClass(className = "org.apache.camel.support.LRUCacheFactory")
final class Target_org_apache_camel_support_LRUCacheFactory {
    @Substitute
    public static <K, V> Map<K, V> newLRUCache(int maximumCacheSize) {
        return new SimpleLRUCache(maximumCacheSize);
    }

    @Substitute
    public static <K, V> Map<K, V> newLRUCache(int maximumCacheSize, Consumer<V> onEvict) {
        return new SimpleLRUCache(16, maximumCacheSize, onEvict);
    }

    @Substitute
    public static <K, V> Map<K, V> newLRUCache(int initialCapacity, int maximumCacheSize) {
        return new SimpleLRUCache(initialCapacity, maximumCacheSize);
    }

    @Substitute
    public static <K, V> Map<K, V> newLRUCache(int initialCapacity, int maximumCacheSize, boolean stopOnEviction) {
        return new SimpleLRUCache(initialCapacity, maximumCacheSize, stopOnEviction);
    }

    @Substitute
    public static <K, V> Map<K, V> newLRUSoftCache(int maximumCacheSize) {
        return new SimpleLRUCache(maximumCacheSize);
    }

    @Substitute
    public static <K, V> Map<K, V> newLRUWeakCache(int maximumCacheSize) {
        return new SimpleLRUCache(maximumCacheSize);
    }

    @Substitute
    public static void warmUp() {
    }

    private static class SimpleLRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maximumCacheSize;
        private final Consumer<V> evict;

        public SimpleLRUCache(int maximumCacheSize) {
            this(16, maximumCacheSize, maximumCacheSize > 0);
        }

        public SimpleLRUCache(int initialCapacity, int maximumCacheSize) {
            this(initialCapacity, maximumCacheSize, maximumCacheSize > 0);
        }

        public SimpleLRUCache(int initialCapacity, int maximumCacheSize, boolean stopOnEviction) {
            this(initialCapacity, maximumCacheSize, stopOnEviction ? SimpleLRUCache::doStop : SimpleLRUCache::doNothing);
        }

        public SimpleLRUCache(int initialCapacity, int maximumCacheSize, Consumer<V> evicted) {
            super(initialCapacity, 0.75F, true);
            this.maximumCacheSize = maximumCacheSize;
            this.evict = Objects.requireNonNull(evicted);
        }

        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            if (this.size() >= this.maximumCacheSize) {
                V value = eldest.getValue();
                this.evict.accept(value);
                return true;
            } else {
                return false;
            }
        }

        static <V> void doNothing(V value) {
        }

        static <V> void doStop(V value) {
            try {
                ServiceHelper.stopService(value);
            } catch (Exception e) {
            }
        }
    }
}