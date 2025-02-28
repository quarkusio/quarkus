package io.quarkus.micrometer.runtime.binder.netty;

import io.micrometer.common.docs.KeyName;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.binder.BaseUnits;
import io.micrometer.core.instrument.docs.MeterDocumentation;

/**
 * Copy of the {@link NettyMeters} enum from the MicroMeter NettyMetrics class in oder to replace the {@code ID} tag
 * with {@code NAME}.
 */
public enum NettyMeters implements MeterDocumentation {

    /**
     * Size of memory used by the allocator, in bytes.
     */
    ALLOCATOR_MEMORY_USED {
        @Override
        public String getName() {
            return "netty.allocator.memory.used";
        }

        @Override
        public Meter.Type getType() {
            return Meter.Type.GAUGE;
        }

        @Override
        public String getBaseUnit() {
            return BaseUnits.BYTES;
        }

        @Override
        public KeyName[] getKeyNames() {
            return KeyName.merge(AllocatorKeyNames.values(), AllocatorMemoryKeyNames.values());
        }
    },

    /**
     * Size of memory used by allocated buffers, in bytes.
     */
    ALLOCATOR_MEMORY_PINNED {
        @Override
        public String getName() {
            return "netty.allocator.memory.pinned";
        }

        @Override
        public Meter.Type getType() {
            return Meter.Type.GAUGE;
        }

        @Override
        public String getBaseUnit() {
            return BaseUnits.BYTES;
        }

        @Override
        public KeyName[] getKeyNames() {
            return KeyName.merge(AllocatorKeyNames.values(), AllocatorMemoryKeyNames.values());
        }
    },

    /**
     * Number of arenas for a pooled allocator.
     */
    ALLOCATOR_POOLED_ARENAS {
        @Override
        public String getName() {
            return "netty.allocator.pooled.arenas";
        }

        @Override
        public Meter.Type getType() {
            return Meter.Type.GAUGE;
        }

        @Override
        public KeyName[] getKeyNames() {
            return KeyName.merge(AllocatorKeyNames.values(), AllocatorMemoryKeyNames.values());
        }
    },

    /**
     * Size of the cache for a pooled allocator, in bytes.
     */
    ALLOCATOR_POOLED_CACHE_SIZE {
        @Override
        public String getName() {
            return "netty.allocator.pooled.cache.size";
        }

        @Override
        public Meter.Type getType() {
            return Meter.Type.GAUGE;
        }

        @Override
        public String getBaseUnit() {
            return BaseUnits.BYTES;
        }

        @Override
        public KeyName[] getKeyNames() {
            return KeyName.merge(AllocatorKeyNames.values(), AllocatorPooledCacheKeyNames.values());
        }
    },

    /**
     * Number of ThreadLocal caches for a pooled allocator.
     */
    ALLOCATOR_POOLED_THREADLOCAL_CACHES {
        @Override
        public String getName() {
            return "netty.allocator.pooled.threadlocal.caches";
        }

        @Override
        public Meter.Type getType() {
            return Meter.Type.GAUGE;
        }

        @Override
        public KeyName[] getKeyNames() {
            return AllocatorKeyNames.values();
        }
    },

    /**
     * Size of memory chunks for a pooled allocator, in bytes.
     */
    ALLOCATOR_POOLED_CHUNK_SIZE {
        @Override
        public String getName() {
            return "netty.allocator.pooled.chunk.size";
        }

        @Override
        public Meter.Type getType() {
            return Meter.Type.GAUGE;
        }

        @Override
        public String getBaseUnit() {
            return BaseUnits.BYTES;
        }

        @Override
        public KeyName[] getKeyNames() {
            return AllocatorKeyNames.values();
        }
    },

    /**
     * Number of pending tasks in the event executor.
     */
    EVENT_EXECUTOR_TASKS_PENDING {
        @Override
        public String getName() {
            return "netty.eventexecutor.tasks.pending";
        }

        @Override
        public Meter.Type getType() {
            return Meter.Type.GAUGE;
        }

        @Override
        public KeyName[] getKeyNames() {
            return EventExecutorTasksPendingKeyNames.values();
        }
    };

    enum AllocatorKeyNames implements KeyName {

        /**
         * Unique runtime identifier for the allocator.
         */
        NAME {
            @Override
            public String asString() {
                return "name";
            }
        },
        /**
         * Allocator's class simple name.
         */
        ALLOCATOR_TYPE {
            @Override
            public String asString() {
                return "allocator.type";
            }
        }

    }

    enum AllocatorMemoryKeyNames implements KeyName {

        /**
         * Type of memory allocated: {@code "heap"} memory or {@code "direct"} memory.
         */
        MEMORY_TYPE {
            @Override
            public String asString() {
                return "memory.type";
            }
        }

    }

    enum AllocatorPooledCacheKeyNames implements KeyName {

        /**
         * Type of cache pages for this cache.
         */
        CACHE_TYPE {
            @Override
            public String asString() {
                return "cache.type";
            }
        }

    }

    enum EventExecutorTasksPendingKeyNames implements KeyName {

        /**
         * Event loop name.
         */
        NAME {
            @Override
            public String asString() {
                return "name";
            }
        }

    }

}