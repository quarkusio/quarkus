package io.quarkus.proxy.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.quarkus.proxy.common.LoggingChannelInboundHandlerAdapter;
import io.quarkus.proxy.common.Ssl;
import io.quarkus.proxy.vertx.VertxUtil;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.impl.ContextInternal;

public class ProxyServer {
    private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);

    private final ContextInternal context;
    private final Ssl.Server ssl;

    /**
     * Constructs a {@link ProxyServer} instance but does not yet start {@link #listen}ing
     * to accept connections.
     *
     * @param context the {@link Context} to use for obtaining {@link io.netty.channel.EventLoop}
     *                to be used for executing handler callbacks
     */
    public ProxyServer(Context context) {
        this(context, null);
    }

    public ProxyServer(Context context, Ssl.Server ssl) {
        this.context = (ContextInternal) Objects.requireNonNull(context, "context");
        this.ssl = ssl;
    }

    /**
     * Starts listening on given host and port.
     *
     * @param host     the bind address (0.0.0.0 for all interfaces/IPs)
     * @param port     the listening port
     * @param handlers the {@link ChannelInboundHandler}s to use for processing channel events.
     *                 Handler's class should be annotated with {@link ChannelHandler.Sharable}
     *                 as it is shared among all channels and therefore invoked in multiple event loop
     *                 group threads.
     * @return a {@link Future} that completes with the outcome of establishing a listening socket
     */
    public Future<Channel> listen(
        String host,
        int port,
        ChannelHandler... handlers
    ) {
        var eventLoop = context.nettyEventLoop();
        var vertx = context.owner();
        var acceptorGroup = vertx.getAcceptorEventLoopGroup();

        var bootstrap = new ServerBootstrap();
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.group(acceptorGroup, eventLoop);

        bootstrap.childHandler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) {
                ChannelPipeline pipeline = ch.pipeline();

                if (Ssl.isEnabled(ssl)) {
                    try (
                        var keyCertChainStream = ssl.keyCertChainUrl().get().openStream();
                        var keyStream = ssl.keyUrl().get().openStream()
                    ) {
                        var sslBuilder = SslContextBuilder
                            .forServer(keyCertChainStream, keyStream);
                        if (ssl.trustCertCollectionUrl().isPresent()) {
                            try (var trustCertCollectionStream = ssl.trustCertCollectionUrl().get().openStream()) {
                                sslBuilder.trustManager(trustCertCollectionStream)
                                          .clientAuth(ssl.clientAuth().orElse(ClientAuth.REQUIRE));
                            }
                        } else {
                            sslBuilder.clientAuth(ssl.clientAuth().orElse(ClientAuth.NONE));
                        }
                        pipeline.addLast(sslBuilder.build().newHandler(ch.alloc()));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                pipeline.addLast(handlers);

                if (log.isDebugEnabled()) {
                    pipeline.addLast(new LoggingChannelInboundHandlerAdapter(log));
                }
            }
        });

        // Bind the server socket
        var channelFuture = bootstrap.bind(host, port);

        return VertxUtil
            .toPromise(context, channelFuture)
            .future()
            .map(v -> channelFuture.channel())
            .onSuccess(ch -> {
                log.info("Proxy server listening on {} using {}",
                         ch.localAddress(), Ssl.isEnabled(ssl) ? "SSL" : "TCP");
            })
            .onFailure(ex -> {
                log.error("Proxy server can't establish listening channel", ex);
            });
    }
}
