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

    // TODO: Remove this method (maybe in 1.6.x of Quarkus or later) if there are no user reports
    // of the WARN message issued from this method. See comments in https://github.com/quarkusio/quarkus/pull/9246
    // for details
    public void eagerlyInitChannelId() {
        //this class is slow to init and can block the IO thread and cause a warning
        //we init it from a throwaway thread to stop this
        //we do it from another thread so as not to affect start time
        new Thread(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                DefaultChannelId.newInstance();
                if (System.currentTimeMillis() - start > 1000) {
                    log.warn("Netty DefaultChannelId initialization (with io.netty.machineId" +
                            " system property set to " + System.getProperty("io.netty.machineId")
                            + ") took more than a second");
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
