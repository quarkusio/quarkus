package io.quarkus.proxy.test.example;

/*

import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.quarkus.proxy.client.ProxyClient;
import io.quarkus.proxy.relay.ProxyRelayProperties;
import io.quarkus.proxy.relay.ServerChannelInboundHandlerAdapter;
import io.quarkus.proxy.server.ProxyServer;
import io.quarkus.proxy.test.support.XMessage;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

**/

public class ProxyRelayConfiguration {

    /*

    private static final Logger log = LoggerFactory.getLogger(ProxyRelayConfiguration.class);

    @Singleton
    @Named("server")
    public Future<Channel> xServerChannel(
        Vertx vertx,
        ProxyRelayProperties props,
        ProxyClient client,
        BiConsumer<ChannelHandlerContext, XMessage> downstreamPeekConsumer
    ) {
        var addr = props.listenAddress().split(":");
        return new ProxyServer(
            vertx.getOrCreateContext(),
            props.listenSsl()
        ).listen(
            addr[0],
            Integer.parseInt(addr[1]),
            new ServerChannelInboundHandlerAdapter(
                client,
                XMessage.class,
                downstreamPeekConsumer,
                (ctx, msg) -> {
                }
            )
        );
    }

    public void startProxyServer(
        @Observes StartupEvent event,
        @Named("server") Future<Channel> proxyServerChannel
    ) {
        proxyServerChannel.onFailure(err -> Quarkus.asyncExit(1));
    }

    public void stopProxyServer(
        @Disposes @Named("server") Future<Channel> proxyServerChannel
    ) {
        log.info("Proxy server shutting down");
        proxyServerChannel.onSuccess(Channel::close);
    }

    @Singleton
    public ProxyClient proxyClient(
        Vertx vertx,
        ProxyRelayProperties props
    ) {
        var addr = props.upstreamAddress().split(":");
        return new ProxyClient(
            vertx.getOrCreateContext(),
            addr[0],
            Integer.parseInt(addr[1]),
            props.upstreamSsl()
        );
    }

    @Singleton
    public BiConsumer<ChannelHandlerContext, XMessage> downstreamPeekConsumer(
        ProxyRelayProperties props
    ) {
        var handler = new XSensorMessageKafkaProducer(
            (short) 1
        );
        return (ctx, msg) -> {
            switch (msg.getType()) {
                case CLOCK_RESPONSE -> handler.clockResponse(ctx, msg);
                default -> {
                    // noop
                }
            }
        };
    }

    // utils

    private static Properties props(Map<String, String> map) {
        var props = new Properties();
        props.putAll(map);
        return props;
    }

     */
}
