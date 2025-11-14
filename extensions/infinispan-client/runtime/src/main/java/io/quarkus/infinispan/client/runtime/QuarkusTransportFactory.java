
package io.quarkus.infinispan.client.runtime;

import java.util.concurrent.ExecutorService;

import org.infinispan.client.hotrod.impl.transport.netty.DefaultTransportFactory;

import io.netty.channel.EventLoopGroup;
import io.quarkus.arc.Arc;
import io.quarkus.netty.MainEventLoopGroup;

/**
 *
 */
public class QuarkusTransportFactory extends DefaultTransportFactory {
    @Override
    public EventLoopGroup createEventLoopGroup(int maxExecutors, ExecutorService executorService) {
        return Arc.container().instance(EventLoopGroup.class, MainEventLoopGroup.Literal.INSTANCE).get();
    }
}
