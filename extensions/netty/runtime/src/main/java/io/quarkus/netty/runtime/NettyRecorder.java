package io.quarkus.netty.runtime;

import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.netty.channel.DefaultChannelId;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class NettyRecorder {

    private static final Logger log = Logger.getLogger(NettyRecorder.class);

    public void eagerlyInitChannelId() {
        //this class is slow to init and can block the IO thread and cause a warning
        //we init it from a throway thread to stop this
        //we do it from another thread so as not to affect start time
        new Thread(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                DefaultChannelId.newInstance();
                if (System.currentTimeMillis() - start > 1000) {
                    log.warn(
                            "Localhost lookup took more than one second, you need to add a /etc/hosts entry to improve Quarkus startup time. See https://thoeni.io/post/macos-sierra-java/ for details.");
                }
            }
        }).start();
    }

    public Supplier<Object> createEventLoop(int nThreads) {
        return new Supplier<Object>() {

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
