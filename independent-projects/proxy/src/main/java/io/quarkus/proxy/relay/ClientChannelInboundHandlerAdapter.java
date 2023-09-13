package io.quarkus.proxy.relay;

import static io.quarkus.proxy.relay.ServerChannelInboundHandlerAdapter.MSG_QUEUE;
import static io.quarkus.proxy.relay.ServerChannelInboundHandlerAdapter.PEER_CTX;

import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientChannelInboundHandlerAdapter<T> extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ClientChannelInboundHandlerAdapter.class);

    private final Class<T> msgType;
    private final ChannelHandlerContext serverCtx;
    private final BiConsumer<? super ChannelHandlerContext, ? super T> downstreamPeekConsumer;
    private final BiConsumer<? super ChannelHandlerContext, ? super T> upstreamPeekConsumer;

    public ClientChannelInboundHandlerAdapter(
        Class<T> msgType,
        ChannelHandlerContext serverCtx,
        BiConsumer<? super ChannelHandlerContext, ? super T> downstreamPeekConsumer,
        BiConsumer<? super ChannelHandlerContext, ? super T> upstreamPeekConsumer
    ) {
        this.msgType = msgType;
        this.serverCtx = serverCtx;
        this.downstreamPeekConsumer = downstreamPeekConsumer;
        this.upstreamPeekConsumer = upstreamPeekConsumer;
    }

    @Override
    public void channelActive(ChannelHandlerContext clientCtx) throws Exception {
        log.info("Proxy client connected: {} -> {}",
                 clientCtx.channel().localAddress(), clientCtx.channel().remoteAddress());

        super.channelActive(clientCtx);
        // interconnect peers
        clientCtx.channel().attr(PEER_CTX).set(serverCtx);
        serverCtx.channel().attr(PEER_CTX).set(clientCtx);
        // flush any messages received by server while client was connecting to peer
        var msgQueue = serverCtx.channel().attr(MSG_QUEUE).getAndSet(null);
        if (msgQueue != null) {
            for (var msg = msgQueue.poll(); msg != null; msg = msgQueue.poll()) {
                var _msg = msg;
                clientCtx
                    .writeAndFlush(msg)
                    .addListener(fut -> {
                        if (fut.isSuccess()) {
                            downstreamPeekConsumer.accept(serverCtx, msgType.cast(_msg));
                        }
                    });
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext clientCtx) throws Exception {
        super.channelInactive(clientCtx);
        var serverCtx = clientCtx.channel().attr(PEER_CTX).get();
        if (serverCtx != null) {
            serverCtx.close();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext clientCtx, Object msg) throws Exception {
        super.channelRead(clientCtx, msg);
        log.info("<<< {}", msg);
        var serverCtx = clientCtx.channel().attr(PEER_CTX).get();
        serverCtx
            .writeAndFlush(msg)
            .addListener(fut -> {
                if (fut.isSuccess() && msgType.isInstance(msg)) {
                    upstreamPeekConsumer.accept(clientCtx, msgType.cast(msg));
                }
            });
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext clientCtx) throws Exception {
        super.channelUnregistered(clientCtx);
        clientCtx.channel().attr(PEER_CTX).set(null);
    }
}
