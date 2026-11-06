package io.quarkus.netty.runtime;

import java.util.concurrent.TimeUnit;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.internal.PlatformDependent;
import io.quarkus.core.StartContext;
import io.quarkus.runtime.util.IsolatedClassInitializer;

public class NettyRecorder {

    /**
     * Initializes, in isolation, the Netty classes whose static initializers store exceptions in static fields:
     * when Unsafe is unavailable, {@code PlatformDependent0} and {@code PlatformDependent} keep an
     * {@code UnsupportedOperationException} whose backtrace references the classes of every frame on the stack when
     * it was created. Initialized from an application thread, they retain the deployment classes that were on the
     * stack, and through them the augmentation class loader, for as long as Netty's own class loader lives.
     *
     * @see IsolatedClassInitializer
     */
    public static void initializePlatformDependent() {
        IsolatedClassInitializer.initialize(PlatformDependent.class.getClassLoader(),
                "io.netty.util.internal.PlatformDependent0",
                "io.netty.util.internal.PlatformDependent");
    }

    public static EventLoopGroup createEventLoop(StartContext ctx, int nThreads) {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(nThreads);
        ctx.onStop(() -> eventLoopGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS));
        return eventLoopGroup;
    }
}
