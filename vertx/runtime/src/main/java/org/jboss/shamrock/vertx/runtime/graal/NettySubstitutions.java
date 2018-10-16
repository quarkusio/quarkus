package org.jboss.shamrock.vertx.runtime.graal;

import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.RecomputeFieldValue.Kind;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ByteToMessageDecoder.Cumulator;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;
import io.vertx.core.net.impl.transport.Transport;

@TargetClass(className = "io.netty.util.internal.shaded.org.jctools.util.UnsafeRefArrayAccess")
final class Target_io_netty_util_internal_shaded_org_jctools_util_UnsafeRefArrayAccess {
     @Alias
     @RecomputeFieldValue(kind = Kind.ArrayIndexShift, declClass = Object[].class)
     public static int REF_ELEMENT_SHIFT;
}

@TargetClass(className = "io.vertx.core.net.impl.transport.Transport")
final class Target_io_vertx_core_net_impl_transport_Transport {
	@Substitute
	public static Transport nativeTransport() {
		return Transport.JDK;
	}
}

@Substitute
@TargetClass(className = "io.netty.handler.ssl.OpenSsl")
final class Target_io_netty_handler_ssl_OpenSsl {
	private static final Throwable UNAVAILABILITY_CAUSE = new NoClassDefFoundError("NO NATIVE SSL ON GRAALVM");
	private static final boolean USE_KEYMANAGER_FACTORY = false;
	
	@Substitute
	public static boolean isAvailable() {
		return false;
	}
	@Substitute
    public static boolean isAlpnSupported() {
    	return false;
    }
	@Substitute
    public static Throwable unavailabilityCause() {
        return UNAVAILABILITY_CAUSE;
    }
	@Substitute
    @SuppressWarnings("unchecked")
	public static Set<String> availableOpenSslCipherSuites() {
        return Collections.EMPTY_SET;
    }
	@Substitute
    public static void ensureAvailability() {
        if (UNAVAILABILITY_CAUSE != null) {
            throw (Error) new UnsatisfiedLinkError(
                    "failed to load the required native library").initCause(UNAVAILABILITY_CAUSE);
        }
    }
	@Substitute
    static boolean useKeyManagerFactory() {
        return USE_KEYMANAGER_FACTORY;
    }
}

@Delete
@TargetClass(className = "io.netty.handler.ssl.ReferenceCountedOpenSslEngine")
final class Target_io_netty_handler_ssl_ReferenceCountedOpenSslEngine {
}

@Delete
@TargetClass(className = "io.netty.handler.ssl.ReferenceCountedOpenSslClientContext")
final class Target_io_netty_handler_ssl_ReferenceCountedOpenSslClientContext {
}

@TargetClass(className = "io.netty.handler.ssl.JdkSslServerContext")
final class Target_io_netty_handler_ssl_JdkSslServerContext{
	@Alias
	Target_io_netty_handler_ssl_JdkSslServerContext(Provider provider,
            X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory,
            X509Certificate[] keyCertChain, PrivateKey key, String keyPassword,
            KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter,
            ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout,
            ClientAuth clientAuth, String[] protocols, boolean startTls) throws SSLException {
	}
}

@TargetClass(className = "io.netty.handler.ssl.JdkSslClientContext")
final class Target_io_netty_handler_ssl_JdkSslClientContext{
	@Alias
	Target_io_netty_handler_ssl_JdkSslClientContext(Provider sslContextProvider,
            X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory,
            X509Certificate[] keyCertChain, PrivateKey key, String keyPassword,
            KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter,
            ApplicationProtocolConfig apn, String[] protocols, long sessionCacheSize, long sessionTimeout)
            		throws SSLException {
    }
}

@TargetClass(className = "io.netty.handler.ssl.SslHandler$SslEngineType")
final class Target_io_netty_handler_ssl_SslHandler$SslEngineType {
	@Alias
	public static Target_io_netty_handler_ssl_SslHandler$SslEngineType JDK;

	@Alias
    Cumulator cumulator;
}

@TargetClass(className = "io.netty.handler.ssl.SslHandler")
final class Target_io_netty_handler_ssl_SslHandler {
	@Alias
    private final SSLEngine engine;
	@Alias
    private final Target_io_netty_handler_ssl_SslHandler$SslEngineType engineType;

	@Alias
    private final Executor delegatedTaskExecutor;
	@Alias
    private final boolean jdkCompatibilityMode;
	@Alias
    private final boolean startTls;
    
	@TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
    @Substitute
	Target_io_netty_handler_ssl_SslHandler(SSLEngine engine, boolean startTls, Executor delegatedTaskExecutor) {
        if (engine == null) {
            throw new NullPointerException("engine");
        }
        if (delegatedTaskExecutor == null) {
            throw new NullPointerException("delegatedTaskExecutor");
        }
        this.engine = engine;
        engineType = Target_io_netty_handler_ssl_SslHandler$SslEngineType.JDK;
        this.delegatedTaskExecutor = delegatedTaskExecutor;
        this.startTls = startTls;
        this.jdkCompatibilityMode = true;
        ((ByteToMessageDecoder)(Object)this).setCumulator(engineType.cumulator);
    }
}

@TargetClass(className = "io.netty.handler.ssl.SslContext")
final class Target_io_netty_handler_ssl_SslContext {
	@Substitute
    static SslContext newServerContextInternal(
            SslProvider provider,
            Provider sslContextProvider,
            X509Certificate[] trustCertCollection, TrustManagerFactory trustManagerFactory,
            X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory,
            Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn,
            long sessionCacheSize, long sessionTimeout, ClientAuth clientAuth, String[] protocols, boolean startTls,
            boolean enableOcsp) throws SSLException {

    	if (enableOcsp) {
    		throw new IllegalArgumentException("OCSP is not supported with this SslProvider: " + provider);
    	}
    	return (SslContext)(Object)new Target_io_netty_handler_ssl_JdkSslServerContext(sslContextProvider,
    			trustCertCollection, trustManagerFactory, keyCertChain, key, keyPassword,
    			keyManagerFactory, ciphers, cipherFilter, apn, sessionCacheSize, sessionTimeout,
    			clientAuth, protocols, startTls);
    }
	
	@Substitute
	static SslContext newClientContextInternal(
	        SslProvider provider,
	        Provider sslContextProvider,
	        X509Certificate[] trustCert, TrustManagerFactory trustManagerFactory,
	        X509Certificate[] keyCertChain, PrivateKey key, String keyPassword, KeyManagerFactory keyManagerFactory,
	        Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, String[] protocols,
	        long sessionCacheSize, long sessionTimeout, boolean enableOcsp) throws SSLException {
		if (enableOcsp) {
			throw new IllegalArgumentException("OCSP is not supported with this SslProvider: " + provider);
		}
		return (SslContext)(Object)new Target_io_netty_handler_ssl_JdkSslClientContext(sslContextProvider,
				trustCert, trustManagerFactory, keyCertChain, key, keyPassword,
				keyManagerFactory, ciphers, cipherFilter, apn, protocols, sessionCacheSize, sessionTimeout);
	}

}


@TargetClass(className = "io.netty.util.internal.logging.InternalLoggerFactory")
final class Target_io_netty_util_internal_logging_InternalLoggerFactory {
	@Substitute
    private static InternalLoggerFactory newDefaultFactory(String name) {
        JdkLoggerFactory f = (JdkLoggerFactory) JdkLoggerFactory.INSTANCE;
        f.newInstance(name).debug("Using java.util.logging as the default logging framework");
        return f;
    }
}

public class NettySubstitutions {

}
