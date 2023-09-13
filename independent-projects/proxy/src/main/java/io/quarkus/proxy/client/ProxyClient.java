package io.quarkus.proxy.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContextBuilder;
import io.quarkus.proxy.common.LoggingChannelInboundHandlerAdapter;
import io.quarkus.proxy.common.Ssl;
import io.quarkus.proxy.vertx.VertxUtil;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.impl.ContextInternal;

public class ProxyClient {
    private static final Logger log = LoggerFactory.getLogger(ProxyClient.class);

    private final ContextInternal context;
    private final String host;
    private final int port;
    private final Ssl ssl;

    public ProxyClient(Context context, String host, int port) {
        this(context, host, port, null);
    }

    public ProxyClient(Context context, String host, int port, Ssl ssl) {
        this.context = (ContextInternal) Objects.requireNonNull(context, "context");
        this.host = Objects.requireNonNull(host, "host");
        this.port = port;
        this.ssl = ssl;
    }

    public Future<Channel> connect(ChannelHandler... handlers) {

        var eventLoop = context.nettyEventLoop();

        var bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(eventLoop);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);

        bootstrap.handler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) {
                ChannelPipeline pipeline = ch.pipeline();

                if (Ssl.isEnabled(ssl)) {
                    try (
                        var keyCertChainStream = ssl.keyCertChainUrl().get().openStream();
                        var keyStream = ssl.keyUrl().get().openStream()
                    ) {
                        var sslBuilder = SslContextBuilder
                            .forClient()
                            .keyManager(keyCertChainStream, keyStream);
                        if (ssl.trustCertCollectionUrl().isPresent()) {
                            try (var trustCertCollectionStream = ssl.trustCertCollectionUrl().get().openStream()) {
                                sslBuilder.trustManager(trustCertCollectionStream);
                            }
                        }
                        pipeline.addLast(sslBuilder.build().newHandler(ch.alloc(), host, port));
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

        // Connect client socket
        var channelFuture = bootstrap.connect(host, port);

        return VertxUtil
            .toPromise(context, channelFuture)
            .future()
            .map(v -> channelFuture.channel())
            .onSuccess(ch -> {
                log.info("Proxy client connected to {} using {}",
                         ch.remoteAddress(), Ssl.isEnabled(ssl) ? "SSL" : "TCP");
            })
            .onFailure(ex -> {
                log.error("Proxy client can't connect", ex);
            });
    }
}
