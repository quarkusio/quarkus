package io.quarkus.netty.runtime;

import java.util.function.Supplier;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.quarkus.runtime.annotations.Recorder;

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
}
