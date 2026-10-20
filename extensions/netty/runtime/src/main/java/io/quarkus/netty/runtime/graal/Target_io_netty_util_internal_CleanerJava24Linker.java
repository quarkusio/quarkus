package io.quarkus.netty.runtime.graal;

import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.word.WordFactory;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Avoid interpreting the runtime-created FFM downcall handles for each allocation and release.
 */
@TargetClass(className = "io.netty.util.internal.CleanerJava24Linker")
final class Target_io_netty_util_internal_CleanerJava24Linker {
    @Substitute
    static boolean isSupported() {
        // Quarkus requires GraalVM 25+ and enables native access for the MemorySegment buffer wrapper.
        return true;
    }

    @Substitute
    static long malloc(int capacity) {
        // Like Netty, allocate at least one byte rather than relying on malloc(0) behavior.
        return UnmanagedMemory.malloc(Math.max(capacity, 1)).rawValue();
    }

    @Substitute
    static void free(long memoryAddress) {
        // UnmanagedMemory allocations must be released through the matching API.
        UnmanagedMemory.free(WordFactory.pointer(memoryAddress));
    }
}
