package io.quarkus.netty.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class HttpContentCompressorSubstitutions {

    @TargetClass(className = "io.netty.handler.codec.compression.ZstdEncoder", onlyWith = IsZstdAbsent.class)
    public static final class ZstdEncoderFactorySubstitution {

        @Substitute
        protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Substitute
        protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
            throw new UnsupportedOperationException();
        }

        @Substitute
        public void flush(final ChannelHandlerContext ctx) {
            throw new UnsupportedOperationException();
        }
    }

    public static class IsZstdAbsent implements BooleanSupplier {

        private boolean zstdAbsent;

        public IsZstdAbsent() {
            try {
                Class.forName("com.github.luben.zstd.Zstd");
                zstdAbsent = false;
            } catch (ClassNotFoundException e) {
                zstdAbsent = true;
            }
        }

        @Override
        public boolean getAsBoolean() {
            return zstdAbsent;
        }
    }
}
