package io.quarkus.proxy.relay;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import io.quarkus.proxy.client.ProxyClient;

@ChannelHandler.Sharable
public class ServerChannelInboundHandlerAdapter<T> extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ServerChannelInboundHandlerAdapter.class);

    static final AttributeKey<ChannelHandlerContext> PEER_CTX =
        AttributeKey.valueOf(ChannelHandlerContext.class, "peer");

    @SuppressWarnings("rawtypes")
    static final AttributeKey<Queue> MSG_QUEUE = AttributeKey.valueOf(Queue.class, "msg");

    private final ProxyClient client;
    private final Class<T> msgType;
    private final BiConsumer<? super ChannelHandlerContext, ? super T> downstreamPeekConsumer;
    private final BiConsumer<? super ChannelHandlerContext, ? super T> upstreamPeekConsumer;

    public ServerChannelInboundHandlerAdapter(
        ProxyClient client,
        Class<T> msgType,
        BiConsumer<? super ChannelHandlerContext, ? super T> downstreamPeekConsumer,
        BiConsumer<? super ChannelHandlerContext, ? super T> upstreamPeekConsumer
    ) {
        this.client = client;
        this.msgType = msgType;
        this.downstreamPeekConsumer = downstreamPeekConsumer;
        this.upstreamPeekConsumer = upstreamPeekConsumer;
    }

    @Override
    public void channelActive(ChannelHandlerContext serverCtx) throws Exception {
        log.info("Proxy server accepted connection: {} -> {}",
                 serverCtx.channel().remoteAddress(), serverCtx.channel().localAddress());

        super.channelActive(serverCtx);
        // set-up message queue to accumulate messages while waiting for client to connect to peer
        serverCtx.channel().attr(MSG_QUEUE).set(new ArrayDeque<>());
        client
            .connect(
                new ClientChannelInboundHandlerAdapter<>(
                    msgType,
                    serverCtx,
                    downstreamPeekConsumer,
                    upstreamPeekConsumer
                )
            )
            .onSuccess(clientChannel -> log.info(
                "Proxy relay established forwarding {} <-> [{} {}] <-> {}",
                serverCtx.channel().remoteAddress(), serverCtx.channel().localAddress(),
                clientChannel.localAddress(), clientChannel.remoteAddress()
            ))
            .onFailure(err -> log.error("Proxy client connection failed", err));
    }

    @Override
    public void channelInactive(ChannelHandlerContext serverCtx) throws Exception {
        super.channelInactive(serverCtx);
        var clientCtx = serverCtx.channel().attr(PEER_CTX).get();
        if (clientCtx != null) {
            clientCtx.close();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext serverCtx, Object msg) throws Exception {
        super.channelRead(serverCtx, msg);
        if (msgType.isInstance(msg)) {
            T tMsg = msgType.cast(msg);
            log.info(">>> {}", tMsg);
            var clientCtx = serverCtx.channel().attr(PEER_CTX).get();
            if (clientCtx != null) {
                clientCtx
                    .writeAndFlush(tMsg)
                    .addListener(fut -> {
                        if (fut.isSuccess()) {
                            downstreamPeekConsumer.accept(serverCtx, tMsg);
                        }
                    });
            } else {
                var msgQueue = serverCtx.channel().attr(MSG_QUEUE).get();
                //noinspection unchecked
                msgQueue.add(tMsg);
            }
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext serverCtx) throws Exception {
        super.channelUnregistered(serverCtx);
        serverCtx.channel().attr(PEER_CTX).set(null);
        serverCtx.channel().attr(MSG_QUEUE).set(null);
    }
}
