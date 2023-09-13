package io.quarkus.proxy.test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.ClientAuth;
import io.quarkus.proxy.client.ProxyClient;
import io.quarkus.proxy.common.Ssl;
import io.quarkus.proxy.server.ProxyServer;
import io.quarkus.proxy.test.support.XMessage;
import io.quarkus.proxy.test.support.XMessageDecoder;
import io.quarkus.proxy.test.support.XMessageEncoder;
import io.vertx.core.Vertx;

@Tag("manual")
public class ProxySslTest {
    private static final Logger log = LoggerFactory.getLogger(ProxySslTest.class);

    @Test
    public void testSsl() throws Exception {
        var vertx = Vertx.vertx();

        var server = new ProxyServer(
            vertx.getOrCreateContext(),
            Ssl.Server.create(
                ProxySslTest.class.getResource("skey.pem"),
                ProxySslTest.class.getResource("scert.pem"),
                ProxySslTest.class.getResource("ccert.pem"),
                ClientAuth.REQUIRE
            )
        );

        var client = new ProxyClient(
            vertx.getOrCreateContext(),
            "localhost", 5999,
            Ssl.create(
                ProxySslTest.class.getResource("ckey.pem"),
                ProxySslTest.class.getResource("ccert.pem"),
                ProxySslTest.class.getResource("scert.pem")
            )
        );

        var finished = new CountDownLatch(1);

        var serverFut = server
            .listen("localhost", 5999,
                new XMessageDecoder(), new XMessageEncoder(),
                new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) {
                    log.info("server channelRead: {}", msg);
                    if (msg instanceof XMessage) {
                        XMessage xm = (XMessage) msg;
                        if (xm.getType() == XMessage.CLOCK_READ) {
                            Instant time = Instant.now();
                            log.info("server got CLOCK_READ: responding with CLOCK_RESPONSE for {}", time);
                            ctx.writeAndFlush(new XMessage(XMessage.CLOCK_RESPONSE, time));
                        }
                    }
                }

                @Override
                public void channelInactive(ChannelHandlerContext ctx) {
                    log.info("server channelInactive: client closed connection");
                    log.info("server closing too...");
                    ctx.channel().close().addListeners(fut -> finished.countDown());
                }
            })
            .onSuccess(serverCh -> {
                client
                    .connect(
                        new XMessageDecoder(), new XMessageEncoder(),
                        new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelActive(ChannelHandlerContext ctx) {
                            log.info("client channelActive: requesting CLOCK_READ");
                            ctx.writeAndFlush(new XMessage(XMessage.CLOCK_READ));
                        }

                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                            log.info("client channelRead: {}", msg);
                            if (msg instanceof XMessage) {
                                XMessage xm = (XMessage) msg;
                                if (xm.getType() == XMessage.CLOCK_RESPONSE) {
                                    log.info("client got CLOCK_RESPONSE: {}", xm.getTime());
                                }
                                log.info("client closing connection...");
                                ctx.channel().close();
                            }
                        }
                    })
                    .onFailure(ex -> log.error("client can't connect", ex));
            })
            .onFailure(ex -> log.error("server can't listen", ex));

        finished.await();
    }
}
