package io.quarkus.pulsar.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Pulsar 4.2.x depends on netty-incubator-transport-native-io_uring:0.0.26.Final
 * which was compiled against Netty 4.1.x. Netty 4.2 removed
 * {@code PlatformDependent.getIntVolatile(long)} causing native image linking
 * failures. These substitutions replace the methods that reference the missing
 * API so GraalVM can link the io_uring classes. The substituted methods are
 * never called at runtime because io_uring is opt-in
 * ({@code -Dpulsar.enableUring=true}) and Linux-only.
 */
@TargetClass(className = "io.netty.incubator.channel.uring.IOUringEventLoop")
final class Target_IOUringEventLoop {

    @Substitute
    protected void run() {
        throw new UnsupportedOperationException("io_uring is not supported with Netty 4.2");
    }

    @Substitute
    protected void cleanup() {
        throw new UnsupportedOperationException("io_uring is not supported with Netty 4.2");
    }
}

@TargetClass(className = "io.netty.incubator.channel.uring.Native")
final class Target_IOUringNative {

    @Substitute
    static Target_RingBuffer createRingBuffer(int ringSize) {
        throw new UnsupportedOperationException("io_uring is not supported with Netty 4.2");
    }

    @Substitute
    static Target_RingBuffer createRingBuffer(int ringSize, int flags) {
        throw new UnsupportedOperationException("io_uring is not supported with Netty 4.2");
    }

    @Substitute
    static Target_RingBuffer createRingBuffer() {
        throw new UnsupportedOperationException("io_uring is not supported with Netty 4.2");
    }
}

@TargetClass(className = "io.netty.incubator.channel.uring.RingBuffer")
final class Target_RingBuffer {
}
