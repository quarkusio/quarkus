package io.quarkus.pulsar.runtime.graal;

import java.util.function.BooleanSupplier;

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
 * <p>
 * Pulsar 5.x no longer depends on the incubator io_uring transport, so these
 * substitutions are only applied when it is present on the classpath.
 */
@TargetClass(className = "io.netty.incubator.channel.uring.IOUringEventLoop", onlyWith = IsIncubatorIOUringPresent.class)
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

@TargetClass(className = "io.netty.incubator.channel.uring.Native", onlyWith = IsIncubatorIOUringPresent.class)
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

@TargetClass(className = "io.netty.incubator.channel.uring.RingBuffer", onlyWith = IsIncubatorIOUringPresent.class)
final class Target_RingBuffer {
}

final class IsIncubatorIOUringPresent implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("io.netty.incubator.channel.uring.IOUringEventLoop", false,
                    Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
