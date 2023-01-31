package io.quarkus.proxy.test.support;

import java.time.Instant;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class XMessageEncoder extends MessageToByteEncoder<XMessage> {
    @Override
    protected void encode(ChannelHandlerContext context, XMessage msg, ByteBuf out) throws Exception {
        out.writeByte(msg.getType());
        if (msg.getType() == XMessage.CLOCK_RESPONSE) {
            Instant time = msg.getTime();
            byte[] bytes = XMessageUtil.encodeData(time);
            out.writeInt(bytes.length);
            out.writeBytes(bytes);
        }
    }
}
