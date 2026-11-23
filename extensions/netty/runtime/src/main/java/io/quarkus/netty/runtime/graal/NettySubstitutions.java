package io.quarkus.netty.runtime.graal;

import static io.netty.handler.codec.http.HttpHeaderValues.BR;
import static io.netty.handler.codec.http.HttpHeaderValues.DEFLATE;
import static io.netty.handler.codec.http.HttpHeaderValues.GZIP;
import static io.netty.handler.codec.http.HttpHeaderValues.SNAPPY;
import static io.netty.handler.codec.http.HttpHeaderValues.X_DEFLATE;
import static io.netty.handler.codec.http.HttpHeaderValues.X_GZIP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.compression.Brotli;
import io.netty.handler.codec.compression.BrotliDecoder;
import io.netty.handler.codec.compression.SnappyFrameDecoder;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkApplicationProtocolNegotiator;
import io.netty.handler.ssl.OpenSslCredential;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextOption;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;
import io.quarkus.netty.runtime.EmptyByteBufStub;

/**
 * This substitution avoid having loggers added to the build
 */
@TargetClass(className = "io.netty.util.internal.logging.InternalLoggerFactory")
final class Target_io_netty_util_internal_logging_InternalLoggerFactory {

    @Substitute
    private static InternalLoggerFactory newDefaultFactory(String name) {
        return JdkLoggerFactory.INSTANCE;
    }
}

// SSL
// This whole section is mostly about removing static analysis references to openssl/tcnative

@TargetClass(className = "io.netty.handler.ssl.SslProvider")
final class Target_io_netty_handler_ssl_SslProvider {
    @Substitute
    public static boolean isAlpnSupported(final SslProvider provider) {
        switch (provider) {
            case JDK:
                return Target_io_netty_handler_ssl_JdkAlpnApplicationProtocolNegotiator.isAlpnSupported();
            case OPENSSL:
            case OPENSSL_REFCNT:
                return false;
            default:
                throw new Error("SslProvider unsupported on Quarkus " + provider);
        }
    }
}

@TargetClass(className = "io.netty.handler.ssl.JdkAlpnApplicationProtocolNegotiator")
final class Target_io_netty_handler_ssl_JdkAlpnApplicationProtocolNegotiator {
    @Alias
    static boolean isAlpnSupported() {
        return true;
    }
}

/**
 * Hardcode io.netty.handler.ssl.OpenSsl as non-available
 */
@TargetClass(className = "io.netty.handler.ssl.OpenSsl")
final class Target_io_netty_handler_ssl_OpenSsl {

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static Throwable UNAVAILABILITY_CAUSE = new RuntimeException("OpenSsl unsupported on Quarkus");

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    static List<String> DEFAULT_CIPHERS = Collections.emptyList();

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    static Set<String> AVAILABLE_CIPHER_SUITES = Collections.emptySet();

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static Set<String> AVAILABLE_OPENSSL_CIPHER_SUITES = Collections.emptySet();

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static Set<String> AVAILABLE_JAVA_CIPHER_SUITES = Collections.emptySet();

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static boolean SUPPORTS_KEYMANAGER_FACTORY = false;

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static boolean USE_KEYMANAGER_FACTORY = false;

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static boolean SUPPORTS_OCSP = false;

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static boolean TLSV13_SUPPORTED = false;

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static boolean IS_BORINGSSL = false;

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static boolean IS_AWSLC = false;

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static Set<String> CLIENT_DEFAULT_PROTOCOLS = Collections.emptySet();

    @Alias
    @RecomputeFieldValue(kind = Kind.FromAlias)
    private static Set<String> SERVER_DEFAULT_PROTOCOLS = Collections.emptySet();

    @Substitute
    public static boolean isAvailable() {
        return false;
    }

    @Substitute
    public static int version() {
        return -1;
    }

    @Substitute
    public static String versionString() {
        return null;
    }

    @Substitute
    public static boolean isCipherSuiteAvailable(String cipherSuite) {
        return false;
    }
}

@TargetClass(className = "io.netty.handler.ssl.JdkSslServerContext")
final class Target_io_netty_handler_ssl_JdkSslServerContext {
    @Alias
    Target_io_netty_handler_ssl_JdkSslServerContext(Provider provider,
            X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory,
            X509Certificate[] keyCertChain, PrivateKey key, String keyPassword,
            KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter,
            ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout,
            ClientAuth clientAuth, String[] protocols, boolean startTls,
            SecureRandom secureRandom, String keyStore, Target_io_netty_handler_ssl_ResumptionController resumptionController)
            throws SSLException {
    }
}

@TargetClass(className = "io.netty.handler.ssl.JdkSslClientContext")
final class Target_io_netty_handler_ssl_JdkSslClientContext {

    @Alias
    Target_io_netty_handler_ssl_JdkSslClientContext(Provider sslContextProvider,
            X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory,
            X509Certificate[] keyCertChain, PrivateKey key, String keyPassword,
            KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter,
            ApplicationProtocolConfig apn, String[] protocols, long sessionCacheSize, long sessionTimeout,
            SecureRandom secureRandom, String keyStoreType, String endpointIdentificationAlgorithm,
            List<SNIServerName> serverNames,
            Target_io_netty_handler_ssl_ResumptionController resumptionController) throws SSLException {
    }
}

@TargetClass(className = "io.netty.handler.ssl.SslHandler$SslEngineType")
final class Target_io_netty_handler_ssl_SslHandler$SslEngineType {

    @Alias
    public static Target_io_netty_handler_ssl_SslHandler$SslEngineType JDK;

    @Substitute
    static Target_io_netty_handler_ssl_SslHandler$SslEngineType forEngine(SSLEngine engine) {
        return JDK;
    }
}

@TargetClass(className = "io.netty.handler.ssl.JdkAlpnApplicationProtocolNegotiator$AlpnWrapper")
final class Target_io_netty_handler_ssl_JdkAlpnApplicationProtocolNegotiator_AlpnWrapper {
    @Substitute
    public SSLEngine wrapSslEngine(SSLEngine engine, ByteBufAllocator alloc,
            JdkApplicationProtocolNegotiator applicationNegotiator, boolean isServer) {
        return (SSLEngine) (Object) new Target_io_netty_handler_ssl_JdkAlpnSslEngine(engine, applicationNegotiator,
                isServer);
    }

}

@TargetClass(className = "io.netty.handler.ssl.JdkAlpnSslEngine")
final class Target_io_netty_handler_ssl_JdkAlpnSslEngine {
    @Alias
    Target_io_netty_handler_ssl_JdkAlpnSslEngine(final SSLEngine engine,
            final JdkApplicationProtocolNegotiator applicationNegotiator, final boolean isServer) {

    }
}

@TargetClass(className = "io.netty.handler.ssl.ResumptionController")
final class Target_io_netty_handler_ssl_ResumptionController {

    @Alias
    Target_io_netty_handler_ssl_ResumptionController() {

    }
}

@TargetClass(className = "io.netty.handler.ssl.SslContext")
final class Target_io_netty_handler_ssl_SslContext {

    @Substitute
    static SslContext newServerContextInternal(SslProvider provider,
            Provider sslContextProvider,
            X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory,
            X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory,
            Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn,
            long sessionCacheSize, long sessionTimeout, ClientAuth clientAuth, String[] protocols, boolean startTls,
            boolean enableOcsp, SecureRandom secureRandom, String keyStoreType,
            Map.Entry<SslContextOption<?>, Object>[] ctxOptions,
            List<OpenSslCredential> credentials) throws SSLException {
        if (enableOcsp) {
            throw new IllegalArgumentException("OCSP is not supported with this SslProvider: " + provider);
        }
        Target_io_netty_handler_ssl_ResumptionController resumptionController = new Target_io_netty_handler_ssl_ResumptionController();
        return (SslContext) (Object) new Target_io_netty_handler_ssl_JdkSslServerContext(sslContextProvider,
                trustCertCollection, trustManagerFactory, keyCertChain, key, keyPassword,
                keyManagerFactory, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout,
                clientAuth, protocols, startTls, secureRandom, keyStoreType, resumptionController);
    }

    @Substitute
    static SslContext newClientContextInternal(SslProvider provider,
            Provider sslContextProvider,
            X509Certificate[] trustCert, TrustManagerFactory trustManagerFactory,
            X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory,
            Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, String[] protocols,
            long sessionCacheSize, long sessionTimeout, boolean enableOcsp,
            SecureRandom secureRandom, String keyStoreType, String endpointIdentificationAlgorithm,
            List<SNIServerName> serverNames,
            Map.Entry<SslContextOption<?>, Object>[] options,
            List<OpenSslCredential> credentials) throws SSLException {
        return newClientContextInternal(provider,
                sslContextProvider,
                trustCert, trustManagerFactory,
                keyCertChain, key, keyPassword, keyManagerFactory,
                ciphers, cipherFilter, apn, protocols,
                sessionCacheSize, sessionTimeout, false, enableOcsp,
                secureRandom, keyStoreType, endpointIdentificationAlgorithm,
                serverNames,
                options, credentials);
    }

    @Substitute
    static SslContext newClientContextInternal(SslProvider provider,
            Provider sslContextProvider,
            X509Certificate[] trustCert, TrustManagerFactory trustManagerFactory,
            X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory,
            Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, String[] protocols,
            long sessionCacheSize, long sessionTimeout, boolean startTls, boolean enableOcsp,
            SecureRandom secureRandom, String keyStoreType, String endpointIdentificationAlgorithm,
            List<SNIServerName> serverNames,
            Map.Entry<SslContextOption<?>, Object>[] options,
            List<OpenSslCredential> credentials) throws SSLException {
        if (enableOcsp) {
            throw new IllegalArgumentException("OCSP is not supported with this SslProvider: " + provider);
        }
        Target_io_netty_handler_ssl_ResumptionController resumptionController = new Target_io_netty_handler_ssl_ResumptionController();
        return (SslContext) (Object) new Target_io_netty_handler_ssl_JdkSslClientContext(sslContextProvider,
                trustCert, trustManagerFactory, keyCertChain, key, keyPassword,
                keyManagerFactory, ciphers, cipherFilter, apn, protocols, sessionCacheSize,
                sessionTimeout, secureRandom, keyStoreType, endpointIdentificationAlgorithm, serverNames,
                resumptionController);
    }

}

// This class is runtime-initialized by NettyProcessor
final class Holder_io_netty_util_concurrent_ScheduledFutureTask {
    static final long START_TIME = System.nanoTime();
}

@TargetClass(className = "io.netty.util.concurrent.AbstractScheduledEventExecutor")
final class Target_io_netty_util_concurrent_AbstractScheduledEventExecutor {

    // The START_TIME field is kept but not used.
    // All the accesses to it have been replaced with Holder_io_netty_util_concurrent_ScheduledFutureTask

    @Substitute
    static long initialNanoTime() {
        return Holder_io_netty_util_concurrent_ScheduledFutureTask.START_TIME;
    }

    @Substitute
    static long defaultCurrentTimeNanos() {
        return System.nanoTime() - Holder_io_netty_util_concurrent_ScheduledFutureTask.START_TIME;
    }
}

@TargetClass(className = "io.netty.channel.ChannelHandlerMask")
final class Target_io_netty_channel_ChannelHandlerMask {

    // Netty tries to self-optimized itself, but it requires lots of reflection. We disable this behavior and avoid
    // misleading DEBUG messages in the log.
    @Substitute
    private static boolean isSkippable(final Class<?> handlerType, final String methodName, final Class... paramTypes) {
        return false;
    }
}

@TargetClass(className = "io.netty.util.internal.NativeLibraryLoader")
final class Target_io_netty_util_internal_NativeLibraryLoader {

    // This method can trick GraalVM into thinking that Classloader#defineClass is getting called
    @Substitute
    static Class<?> tryToLoadClass(final ClassLoader loader, final Class<?> helper)
            throws ClassNotFoundException {
        return Class.forName(helper.getName(), false, loader);
    }

}

@TargetClass(className = "io.netty.buffer.EmptyByteBuf")
final class Target_io_netty_buffer_EmptyByteBuf {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static ByteBuffer EMPTY_BYTE_BUFFER;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static long EMPTY_BYTE_BUFFER_ADDRESS;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private ByteBufAllocator alloc;

    @Substitute
    public ByteBufAllocator alloc() {
        return UnpooledByteBufAllocator.DEFAULT;
    }

    @Substitute
    public ByteBuffer nioBuffer() {
        return EmptyByteBufStub.emptyByteBuffer();
    }

    @Substitute
    public ByteBuffer[] nioBuffers() {
        return new ByteBuffer[] { EmptyByteBufStub.emptyByteBuffer() };
    }

    @Substitute
    public ByteBuffer internalNioBuffer(int index, int length) {
        return EmptyByteBufStub.emptyByteBuffer();
    }

    @Substitute
    public boolean hasMemoryAddress() {
        return EmptyByteBufStub.emptyByteBufferAddress() != 0;
    }

    @Substitute
    public long memoryAddress() {
        if (hasMemoryAddress()) {
            return EmptyByteBufStub.emptyByteBufferAddress();
        } else {
            throw new UnsupportedOperationException();
        }
    }

}

@TargetClass(className = "io.netty.handler.codec.http.HttpContentDecompressor")
final class Target_io_netty_handler_codec_http_HttpContentDecompressor {

    @Alias
    private boolean strict;

    @Alias
    private int maxAllocation;

    @Alias
    protected ChannelHandlerContext ctx;

    @Substitute
    protected EmbeddedChannel newContentDecoder(String contentEncoding) throws Exception {
        if (GZIP.contentEqualsIgnoreCase(contentEncoding) ||
                X_GZIP.contentEqualsIgnoreCase(contentEncoding)) {
            return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
                    ctx.channel().config(), ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP, maxAllocation));
        }
        if (DEFLATE.contentEqualsIgnoreCase(contentEncoding) ||
                X_DEFLATE.contentEqualsIgnoreCase(contentEncoding)) {
            final ZlibWrapper wrapper = strict ? ZlibWrapper.ZLIB : ZlibWrapper.ZLIB_OR_NONE;
            // To be strict, 'deflate' means ZLIB, but some servers were not implemented correctly.
            return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
                    ctx.channel().config(), ZlibCodecFactory.newZlibDecoder(wrapper, maxAllocation));
        }
        if (Brotli.isAvailable() && BR.contentEqualsIgnoreCase(contentEncoding)) {
            return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
                    ctx.channel().config(), BrotliDecoder.newDecoderWithMaxAllocation(maxAllocation));
        }
        if (SNAPPY.contentEqualsIgnoreCase(contentEncoding)) {
            return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
                    ctx.channel().config(), new SnappyFrameDecoder());
        }
        // 'identity' or unsupported
        return null;
    }
}

@TargetClass(className = "io.netty.handler.codec.http2.DelegatingDecompressorFrameListener")
final class Target_io_netty_handler_codec_http2_DelegatingDecompressorFrameListener {

    @Alias
    boolean strict;

    @Alias
    int maxAllocation;

    @Substitute
    protected EmbeddedChannel newContentDecompressor(ChannelHandlerContext ctx, CharSequence contentEncoding)
            throws Http2Exception {
        if (GZIP.contentEqualsIgnoreCase(contentEncoding) || X_GZIP.contentEqualsIgnoreCase(contentEncoding)) {
            return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
                    ctx.channel().config(), ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP, maxAllocation));
        }
        if (DEFLATE.contentEqualsIgnoreCase(contentEncoding) || X_DEFLATE.contentEqualsIgnoreCase(contentEncoding)) {
            final ZlibWrapper wrapper = strict ? ZlibWrapper.ZLIB : ZlibWrapper.ZLIB_OR_NONE;
            return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
                    ctx.channel().config(), ZlibCodecFactory.newZlibDecoder(wrapper, maxAllocation));
        }
        if (Brotli.isAvailable() && BR.contentEqualsIgnoreCase(contentEncoding)) {
            return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
                    ctx.channel().config(), new BrotliDecoder(maxAllocation));
        }
        if (SNAPPY.contentEqualsIgnoreCase(contentEncoding)) {
            return new EmbeddedChannel(ctx.channel().id(), ctx.channel().metadata().hasDisconnect(),
                    ctx.channel().config(), new SnappyFrameDecoder());
        }
        // 'identity' or unsupported
        return null;
    }
}

@TargetClass(className = "io.netty.handler.ssl.SslHandler")
final class Target_SslHandler {

    @Substitute
    private void setOpensslEngineSocketFd(Channel c) {
        // do nothing.
    }
}

@TargetClass(className = "io.netty.handler.ssl.PemReader")
final class Alias_PemReader {

    @Alias
    public static ByteBuf readPrivateKey(File keyFile) {
        return null;
    }

    @Alias
    public static ByteBuf readPrivateKey(InputStream in) throws KeyException {
        return null;
    }
}

/**
 * If BouncyCastle is not on the classpath, we must not try to read the PEM file using the BouncyCatle PEM reader.
 */
@TargetClass(className = "io.netty.handler.ssl.SslContext", onlyWith = IsBouncyNotThere.class)
final class Target_SslContext {

    @Substitute
    protected static PrivateKey toPrivateKey(File keyFile, String keyPassword) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeySpecException,
            InvalidAlgorithmParameterException,
            KeyException, IOException {
        if (keyFile == null) {
            return null;
        }

        return getPrivateKeyFromByteBuffer(Alias_PemReader.readPrivateKey(keyFile), keyPassword);
    }

    @Substitute
    protected static PrivateKey toPrivateKey(InputStream keyInputStream, String keyPassword)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeySpecException,
            InvalidAlgorithmParameterException,
            KeyException, IOException {
        if (keyInputStream == null) {
            return null;
        }

        return getPrivateKeyFromByteBuffer(Alias_PemReader.readPrivateKey(keyInputStream), keyPassword);
    }

    @Alias
    private static PrivateKey getPrivateKeyFromByteBuffer(ByteBuf encodedKeyBuf, String keyPassword)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, KeyException, IOException {
        return null;
    }
}

@TargetClass(className = "io.netty.util.internal.shaded.org.jctools.util.UnsafeLongArrayAccess")
final class Target_io_netty_util_internal_shaded_org_jctools_util_UnsafeLongArrayAccess {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.ArrayIndexShift, declClass = long[].class)
    public static int LONG_ELEMENT_SHIFT;
}

class IsBouncyNotThere implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            NettySubstitutions.class.getClassLoader().loadClass("org.bouncycastle.openssl.PEMParser");
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}

class NettySubstitutions {

}
