package io.quarkus.it.netty;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.util.NettyRuntime;
import io.quarkus.netty.MainEventLoopGroup;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Prints the values Netty sized itself with, next to the run-time environment they should be derived from.
 */
@QuarkusMain
@RegisterForReflection(classNames = NettySizingApp.ADAPTIVE_POOLING_ALLOCATOR, fields = true)
public class NettySizingApp implements QuarkusApplication {

    static final String ADAPTIVE_POOLING_ALLOCATOR = "io.netty.buffer.AdaptivePoolingAllocator";

    @Inject
    @MainEventLoopGroup
    EventLoopGroup mainEventLoopGroup;

    @Override
    public int run(String... args) throws Exception {
        ByteBufAllocator.DEFAULT.directBuffer(1).release();

        print("runtime.availableProcessors", Runtime.getRuntime().availableProcessors());
        print("runtime.maxMemory", Runtime.getRuntime().maxMemory());
        print("runtime.magazineBufferQueueCapacity", System.getProperty("io.netty.allocator.magazineBufferQueueCapacity"));

        print("netty.availableProcessors", NettyRuntime.availableProcessors());
        print("netty.allocator", ByteBufAllocator.DEFAULT.getClass().getName());
        print("netty.mainEventLoopGroup.executors", executors(mainEventLoopGroup));
        EventLoopGroup defaultSizedGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            print("netty.defaultSizedEventLoopGroup.executors", executors(defaultSizedGroup));
        } finally {
            defaultSizedGroup.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS);
        }

        // Netty does not expose the allocator sizing: if these fields are renamed, the application fails here
        Class<?> adaptivePoolingAllocator = Class.forName(ADAPTIVE_POOLING_ALLOCATOR);
        print("netty.adaptive.lowMemory", staticField(adaptivePoolingAllocator, "IS_LOW_MEM"));
        print("netty.adaptive.chunkReuseQueueCapacity", staticField(adaptivePoolingAllocator, "CHUNK_REUSE_QUEUE"));
        print("netty.adaptive.magazineBufferQueueCapacity",
                staticField(adaptivePoolingAllocator, "MAGAZINE_BUFFER_QUEUE_CAPACITY"));
        return 0;
    }

    private static void print(String key, Object value) {
        System.out.println(key + "=" + value);
    }

    private static int executors(EventLoopGroup group) {
        int executors = 0;
        for (var ignored : group) {
            executors++;
        }
        return executors;
    }

    private static Object staticField(Class<?> clazz, String name) throws ReflectiveOperationException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }
}
