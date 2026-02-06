package io.quarkus.netty.runtime;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.function.Supplier;

import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.StaticInit;

@Recorder
public class NettyRecorder {

    public Supplier<EventLoopGroup> createEventLoop(int nThreads) {
        return new Supplier<EventLoopGroup>() {

            volatile EventLoopGroup val;

            @Override
            public EventLoopGroup get() {
                if (val == null) {
                    synchronized (this) {
                        if (val == null) {
                            val = new NioEventLoopGroup(nThreads);
                        }
                    }
                }
                return val;
            }
        };
    }

    private static final VarHandle CHANNEL_HANDLER_MASKS;
    private static volatile Map<String, Integer> channelHandlerMask;

    static {
        try {
            CHANNEL_HANDLER_MASKS = MethodHandles.lookup()
                    .findStaticVarHandle(NettyRecorder.class, "channelHandlerMask", Map.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @StaticInit
    public void setChannelHandlerMasks(Map<String, Integer> masks) {
        CHANNEL_HANDLER_MASKS.setRelease(masks);
    }

    @SuppressWarnings("unused") // used in generated code
    public static Integer channelHandlerMask(Class<? extends ChannelHandler> handlerType) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> map = (Map<String, Integer>) CHANNEL_HANDLER_MASKS.getAcquire();
        if (map == null) {
            return null;
        }
        return map.get(handlerType.getName());
    }

    public void cleanUp(ShutdownContext shutdown) {
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                channelHandlerMask = null;
            }
        });

    }
}
