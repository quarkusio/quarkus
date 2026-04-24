package io.quarkus.netty.runtime;

import java.util.function.Supplier;

/**
 * Computes a sensible default for {@code io.netty.allocator.numDirectArenas} at runtime,
 * capping the value to avoid excessive memory usage on machines with many cores.
 */
public final class DefaultNumDirectArenasGenerator implements Supplier<String> {

    private static final int MAX_DEFAULT_ARENA_CPU_COUNT = 16;

    @Override
    public String get() {
        int cpus = Runtime.getRuntime().availableProcessors();
        int arenas = 2 * Math.min(cpus, MAX_DEFAULT_ARENA_CPU_COUNT);
        return Integer.toString(arenas);
    }
}
