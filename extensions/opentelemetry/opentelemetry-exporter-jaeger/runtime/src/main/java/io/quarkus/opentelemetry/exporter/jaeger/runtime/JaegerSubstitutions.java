package io.quarkus.opentelemetry.exporter.jaeger.runtime;

import static java.util.Objects.requireNonNull;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.opentelemetry.exporter.internal.grpc.ManagedChannelUtil;

/**
 * Replace the {@link ManagedChannelUtil#setClientKeysAndTrustedCertificatesPem(ManagedChannelBuilder, byte[], byte[], byte[])}
 * method in native
 * because the method implementation tries to look for grpc-netty-shaded dependencies, which we don't support.
 *
 * Check:
 * https://github.com/open-telemetry/opentelemetry-java/blob/v1.13.0/exporters/otlp/common/src/main/java/io/opentelemetry/exporter/internal/grpc/ManagedChannelUtil.java#L47-L91
 */
final class JaegerSubstitutions {
    @TargetClass(ManagedChannelUtil.class)
    static final class Target_ManagedChannelUtil {
        @Substitute
        public static void setClientKeysAndTrustedCertificatesPem(
                ManagedChannelBuilder<?> managedChannelBuilder, byte[] privateKeyPem, byte[] certificatePem,
                byte[] trustedCertificatesPem)
                throws SSLException {
            requireNonNull(managedChannelBuilder, "managedChannelBuilder");
            requireNonNull(trustedCertificatesPem, "trustedCertificatesPem");

            X509TrustManager tm = io.opentelemetry.exporter.internal.TlsUtil.trustManager(trustedCertificatesPem);

            // gRPC does not abstract TLS configuration so we need to check the implementation and act
            // accordingly.
            if (managedChannelBuilder.getClass().getName().equals("io.grpc.netty.NettyChannelBuilder")) {
                NettyChannelBuilder nettyBuilder = (NettyChannelBuilder) managedChannelBuilder;
                nettyBuilder.sslContext(GrpcSslContexts.forClient().trustManager(tm).build());
            } else {
                throw new SSLException(
                        "TLS certificate configuration not supported for unrecognized ManagedChannelBuilder "
                                + managedChannelBuilder.getClass().getName());
            }
        }
    }
}
