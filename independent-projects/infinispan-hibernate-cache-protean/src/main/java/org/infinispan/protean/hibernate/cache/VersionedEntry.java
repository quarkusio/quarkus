package org.infinispan.protean.hibernate.cache;

import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.StructuredCacheEntry;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

final class VersionedEntry {

    private static final Logger log = Logger.getLogger(VersionedEntry.class);
    private static final boolean trace = log.isTraceEnabled();

    public static final Duration TOMBSTONE_LIFESPAN = Duration.ofSeconds(60);

    static final ExcludeEmptyFilter EXCLUDE_EMPTY_VERSIONED_ENTRY = new ExcludeEmptyFilter();

    private final Object value;
    private final Object version;
    private final long timestamp;
    private Duration lifespan = Duration.ofSeconds(Long.MAX_VALUE);

    VersionedEntry(Object value, Object version, long timestamp) {
        this.value = value;
        this.version = version;
        this.timestamp = timestamp;
    }

    VersionedEntry(long timestamp) {
        this(null, null, timestamp);
    }

    VersionedEntry(long timestamp, Duration lifespan) {
        this(null, null, timestamp);
        this.lifespan = lifespan;
    }

    Object getVersion() {
        return version;
    }

    long getTimestamp() {
        return timestamp;
    }

    long getLifespanNanos() {
        return lifespan.toNanos();
    }

    Object getValue() {
        return value;
    }

    static final class ComputeFn implements BiFunction<Object, Object, Object> {

        final VersionedEntry entry;
        final InternalRegion region;

        ComputeFn(VersionedEntry entry, InternalRegion region) {
            this.entry = entry;
            this.region = region;
        }

        @Override
        public Object apply(Object key, Object oldValue) {
            if (trace) {
                log.tracef("Applying %s to %s", this, oldValue);
            }
            if (entry.version == null) {
                // eviction or post-commit removal: we'll store it with given timestamp
                entry.lifespan = TOMBSTONE_LIFESPAN;
                return entry;
            }

            Object oldVersion;
            long oldTimestamp = Long.MIN_VALUE;
            if (oldValue instanceof VersionedEntry) {
                VersionedEntry oldVersionedEntry = (VersionedEntry) oldValue;
                oldVersion = oldVersionedEntry.version;
                oldTimestamp = oldVersionedEntry.timestamp;
                oldValue = oldVersionedEntry.value;
            } else {
                oldVersion = findVersion(oldValue);
            }

            if (oldVersion == null) {
                assert oldValue == null || oldTimestamp != Long.MIN_VALUE : oldValue;
                if (entry.timestamp <= oldTimestamp) {
                    // either putFromLoad or regular update/insert - in either case this update might come
                    // when it was evicted/region-invalidated. In both cases, with old timestamp we'll leave
                    // the invalid value
                    assert oldValue == null;
                    return null;
                } else {
                    return entry.value instanceof CacheEntry ? entry.value : entry;
                }
            } else {
                Comparator<Object> versionComparator = null;
                String subclass = findSubclass(entry.value);
                if (subclass != null) {
                    versionComparator = region.getComparator(subclass);
                    if (versionComparator == null) {
                        log.errorf("Cannot find comparator for %s", subclass);
                    }
                }
                if (versionComparator == null) {
                    entry.lifespan = TOMBSTONE_LIFESPAN;
                    return new VersionedEntry(null, null, entry.timestamp);
                } else {
                    int compareResult = versionComparator.compare(entry.version, oldVersion);
                    if (trace) {
                        log.tracef("Comparing %s and %s -> %d (using %s)", entry.version, oldVersion, compareResult, versionComparator);
                    }
                    if (entry.value == null && compareResult >= 0) {
                        entry.lifespan = TOMBSTONE_LIFESPAN;
                        return entry;
                    } else if (compareResult > 0) {
                        return entry.value instanceof CacheEntry ? entry.value : entry;
                    } else {
                        return oldValue;
                    }
                }
            }
        }

        private static Object findVersion(Object entry) {
            if (entry instanceof CacheEntry) {
                // with UnstructuredCacheEntry
                return ((CacheEntry) entry).getVersion();
            } else if (entry instanceof Map) {
                return ((Map) entry).get(StructuredCacheEntry.VERSION_KEY);
            } else {
                return null;
            }
        }

        private static String findSubclass(Object entry) {
            // we won't find subclass for structured collections
            if (entry instanceof CacheEntry) {
                return ((CacheEntry) entry).getSubclass();
            } else if (entry instanceof Map) {
                Object maybeSubclass = ((Map) entry).get(StructuredCacheEntry.SUBCLASS_KEY);
                return maybeSubclass instanceof String ? (String) maybeSubclass : null;
            } else {
                return null;
            }
        }

    }

    private static class ExcludeEmptyFilter implements Predicate<Map.Entry> {

        @Override
        public boolean test(Map.Entry entry) {
            if (entry.getValue() instanceof VersionedEntry) {
                return ((VersionedEntry) entry.getValue()).getValue() != null;
            }
            return true;
        }

    }

}
