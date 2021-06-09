package io.quarkus.infinispan.client.runtime.graal;

import java.util.concurrent.ExecutorService;

import org.infinispan.client.hotrod.impl.transport.netty.DefaultTransportFactory;

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
@TargetClass(DefaultTransportFactory.class)
final class SubstituteDefaultTransportFactory {

    @Substitute
    public Class<? extends SocketChannel> socketChannelClass() {
        return NioSocketChannel.class;
    }

    @Substitute
    public EventLoopGroup createEventLoopGroup(int maxExecutors, ExecutorService executorService) {
        return new NioEventLoopGroup(maxExecutors, executorService);
    }
}
