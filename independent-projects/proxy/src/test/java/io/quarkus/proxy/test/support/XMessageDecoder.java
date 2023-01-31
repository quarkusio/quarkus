package io.quarkus.proxy.test.support;

import java.time.Instant;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class XMessageDecoder extends ReplayingDecoder<XMessage> {
    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf in, List<Object> out) throws Exception {
        byte type = in.readByte();
        if (type == XMessage.CLOCK_RESPONSE) {
            int n = in.readInt();
            byte[] bytes = new byte[n];
            in.readBytes(bytes);
            Instant time = XMessageUtil.decodeData(bytes);
            out.add(new XMessage(type, time));
        } else {
            out.add(new XMessage(type));
        }
    }
}
