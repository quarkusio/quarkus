package io.quarkus.opentelemetry.runtime.exporter.otlp.graal;

import static java.util.Objects.requireNonNull;

import javax.net.ssl.SSLException;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;

/**
 * Replace the {@code setTrustedCertificatesPem()} method in native because the upstream code supports using
 * either the grpc-netty or grpc-netty-shaded dependencies, but Quarkus only supports the former.
 */
@TargetClass(className = "io.opentelemetry.exporter.internal.grpc.ManagedChannelUtil")
final class Target_io_opentelemetry_exporter_otlp_internal_grpc_ManagedChannelUtil {

    @Substitute
    public static void setClientKeysAndTrustedCertificatesPem(
            ManagedChannelBuilder<?> managedChannelBuilder,
            X509TrustManager tmf,
            X509KeyManager kmf)
            throws SSLException {
        requireNonNull(managedChannelBuilder, "managedChannelBuilder");
        requireNonNull(tmf, "X509TrustManager");

        // gRPC does not abstract TLS configuration so we need to check the implementation and act
        // accordingly.
        if (managedChannelBuilder.getClass().getName().equals("io.grpc.netty.NettyChannelBuilder")) {
            NettyChannelBuilder nettyBuilder = (NettyChannelBuilder) managedChannelBuilder;
            nettyBuilder.sslContext(
                    GrpcSslContexts.forClient().keyManager(kmf).trustManager(tmf).build());
        } else {
            throw new SSLException(
                    "TLS certificate configuration not supported for unrecognized ManagedChannelBuilder "
                            + managedChannelBuilder.getClass().getName());
        }
    }
}
