package io.quarkus.proxy.common;

import org.slf4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LoggingChannelInboundHandlerAdapter extends ChannelInboundHandlerAdapter {
    private final Logger log;

    public LoggingChannelInboundHandlerAdapter(Logger log) {
        this.log = log;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        log.debug("channelRegistered({}): {} -> {}",
                  ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        log.debug("channelUnregistered({}): {} -> {}",
                  ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("channelActive({}): {} -> {}",
                  ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("channelInactive({}): {} -> {}",
                  ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("channelRead({}, {}): {} -> {}",
                  ctx.channel().id(), msg, ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        log.debug("channelReadComplete({}): {} -> {}",
                  ctx.channel().id(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        log.debug("channelWritabilityChanged({}, {}): {} -> {}",
                  ctx.channel().id(), ctx.channel().isWritable(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        log.debug("userEventTriggered({}, {}): {} -> {}",
                  ctx.channel().id(), evt, ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.debug("exceptionCaught({}, {}): {} -> {}",
                  ctx.channel().id(), cause.toString(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
    }
}
