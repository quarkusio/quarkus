package io.quarkus.netty.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class HttpContentCompressorSubstitutions {
}

@TargetClass(className = "io.netty.handler.codec.compression.ZstdEncoder", onlyWith = IsZstdAbsent.class)
final class Target_io_netty_handler_codec_compression_ZstdEncoder {

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

@Substitute
@TargetClass(className = "io.netty.handler.codec.compression.ZstdConstants", onlyWith = IsZstdAbsent.class)
final class Target_io_netty_handler_codec_compression_ZstdConstants {

    // The constants make <clinit> calls to com.github.luben.zstd.Zstd so we cut links with that substitution.

    static final int DEFAULT_COMPRESSION_LEVEL = 0;

    static final int MIN_COMPRESSION_LEVEL = 0;

    static final int MAX_COMPRESSION_LEVEL = 0;

    static final int MAX_BLOCK_SIZE = 0;

    static final int DEFAULT_BLOCK_SIZE = 0;
}

class IsZstdAbsent implements BooleanSupplier {

    private boolean zstdAbsent;

    public IsZstdAbsent() {
        try {
            Class.forName("com.github.luben.zstd.Zstd");
            zstdAbsent = false;
        } catch (Exception e) {
            // It can be a classloading issue (the library is not available), or a native issue
            // (the library for the current OS/arch is not available)
            zstdAbsent = true;
        }
    }

    @Override
    public boolean getAsBoolean() {
        return zstdAbsent;
    }
}
