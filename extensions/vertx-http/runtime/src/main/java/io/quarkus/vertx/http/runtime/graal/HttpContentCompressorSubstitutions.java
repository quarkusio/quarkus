package io.quarkus.vertx.http.runtime.graal;

import static io.netty.handler.codec.http.HttpHeaderValues.DEFLATE;
import static io.netty.handler.codec.http.HttpHeaderValues.GZIP;
import static io.netty.handler.codec.http.HttpHeaderValues.X_DEFLATE;
import static io.netty.handler.codec.http.HttpHeaderValues.X_GZIP;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http2.CompressorHttp2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Exception;

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

    @TargetClass(className = "io.netty.handler.codec.compression.BrotliEncoder", onlyWith = IsBrotliAbsent.class)
    public static final class BrEncoderFactorySubstitution {

        @Substitute
        protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ByteBuf msg, boolean preferDirect) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Substitute
        protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) {
            throw new UnsupportedOperationException();
        }
    }

    @TargetClass(CompressorHttp2ConnectionEncoder.class)
    public static final class CompressorHttp2ConnectionSubstitute {

        @Substitute
        protected EmbeddedChannel newContentCompressor(ChannelHandlerContext ctx, CharSequence contentEncoding)
                throws Http2Exception {
            if (GZIP.contentEqualsIgnoreCase(contentEncoding) || X_GZIP.contentEqualsIgnoreCase(contentEncoding)) {
                return newCompressionChannel(ctx, ZlibWrapper.GZIP);
            }
            if (DEFLATE.contentEqualsIgnoreCase(contentEncoding) || X_DEFLATE.contentEqualsIgnoreCase(contentEncoding)) {
                return newCompressionChannel(ctx, ZlibWrapper.ZLIB);
            }
            // 'identity' or unsupported
            return null;
        }

        @Alias
        private EmbeddedChannel newCompressionChannel(final ChannelHandlerContext ctx, ZlibWrapper wrapper) {
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

    public static class IsBrotliAbsent implements BooleanSupplier {

        private boolean brotliAbsent;

        public IsBrotliAbsent() {
            try {
                Class.forName("com.aayushatharva.brotli4j.encoder.Encoder");
                brotliAbsent = false;
            } catch (ClassNotFoundException e) {
                brotliAbsent = true;
            }
        }

        @Override
        public boolean getAsBoolean() {
            return brotliAbsent;
        }
    }
}
