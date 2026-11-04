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

    static final int DEFAULT_MAX_ENCODE_SIZE = 0;

    static final int DEFAULT_BLOCK_SIZE = 0;
}

@Substitute
@TargetClass(className = "io.netty.handler.codec.compression.Brotli", onlyWith = IsBrotli4jAbsent.class)
final class Target_io_netty_handler_codec_compression_Brotli {

    // The original class references com.aayushatharva.brotli4j.Brotli4jLoader, so we cut links with that substitution.

    @Substitute
    public static boolean isAvailable() {
        return false;
    }

    @Substitute
    public static void ensureAvailability() throws Throwable {
        throw cause();
    }

    @Substitute
    public static Throwable cause() {
        return new ClassNotFoundException("com.aayushatharva.brotli4j.Brotli4jLoader");
    }
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

class IsBrotli4jAbsent implements BooleanSupplier {

    // WARNING: There are other copies of this class:
    // - io.quarkus.vertx.http.runtime.graal.IsBrotli4jAbsent.
    // - io.quarkus.elasticsearch.restclient.common.runtime.graal.IsBrotli4jAbsent.
    // If you change this class, make sure to change the other ones as well.

    @Override
    public boolean getAsBoolean() {
        try {
            // Do not initialize the class: its static initializer loads the native library.
            Class.forName("com.aayushatharva.brotli4j.Brotli4jLoader", false, IsBrotli4jAbsent.class.getClassLoader());
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
