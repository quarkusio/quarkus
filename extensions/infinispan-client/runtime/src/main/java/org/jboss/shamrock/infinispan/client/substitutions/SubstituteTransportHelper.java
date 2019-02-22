package io.quarkus.infinispan.client.substitutions;

import java.util.concurrent.ExecutorService;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * This class is here so there are no code paths to loading up native epoll
 * 
 * @author William Burns
 */
@Substitute
@TargetClass(className = "org.infinispan.client.hotrod.impl.transport.netty.TransportHelper")
final class SubstituteTransportHelper {

    @Substitute
    static Class<? extends SocketChannel> socketChannel() {
        return NioSocketChannel.class;
    }

    @Substitute
    static EventLoopGroup createEventLoopGroup(int maxExecutors, ExecutorService executorService) {
        return new NioEventLoopGroup(maxExecutors, executorService);
    }
}
