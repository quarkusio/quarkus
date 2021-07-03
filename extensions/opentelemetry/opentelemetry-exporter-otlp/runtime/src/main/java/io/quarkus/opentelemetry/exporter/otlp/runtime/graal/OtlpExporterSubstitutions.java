package io.quarkus.opentelemetry.exporter.otlp.runtime.graal;

import java.io.ByteArrayInputStream;
import java.net.URI;

import javax.net.ssl.SSLException;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.MetadataUtils;

/**
 * Replace the {@code build()} method in native because the upstream code supports using
 * either the grpc-netty or grpc-netty-shaded dependencies, but Quarkus only supports the former.
 */
@TargetClass(className = "io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder")
final class Target_io_opentelemetry_exporter_otlp_trace_OtlpGrpcSpanExporterBuilder {

    @Alias
    private ManagedChannel channel;

    @Alias
    private long timeoutNanos;

    @Alias
    private URI endpoint;

    @Alias
    private Metadata metadata;

    @Alias
    private byte[] trustedCertificatesPem;

    @Substitute
    public Target_io_opentelemetry_exporter_otlp_trace_OtlpGrpcSpanExporter build() {
        if (channel == null) {
            final ManagedChannelBuilder<?> managedChannelBuilder = ManagedChannelBuilder.forTarget(endpoint.getAuthority());

            if (endpoint.getScheme().equals("https")) {
                managedChannelBuilder.useTransportSecurity();
            } else {
                managedChannelBuilder.usePlaintext();
            }

            if (metadata != null) {
                managedChannelBuilder.intercept(MetadataUtils.newAttachHeadersInterceptor(metadata));
            }

            if (trustedCertificatesPem != null) {
                // gRPC does not abstract TLS configuration so we need to check the implementation and act
                // accordingly.
                if (managedChannelBuilder
                        .getClass()
                        .getName()
                        .equals("io.grpc.netty.NettyChannelBuilder")) {
                    NettyChannelBuilder nettyBuilder = (NettyChannelBuilder) managedChannelBuilder;
                    try {
                        nettyBuilder.sslContext(
                                GrpcSslContexts.forClient()
                                        .trustManager(new ByteArrayInputStream(trustedCertificatesPem))
                                        .build());
                    } catch (IllegalArgumentException | SSLException e) {
                        throw new IllegalStateException(
                                "Could not set trusted certificates for gRPC TLS connection, are they valid "
                                        + "X.509 in PEM format?",
                                e);
                    }
                } else {
                    throw new IllegalStateException(
                            "TLS certificate configuration only supported with Netty. "
                                    + "If you need to configure a certificate, switch to grpc-netty or "
                                    + "grpc-netty-shaded.");
                }
                // TODO(anuraaga): Support okhttp.
            }

            channel = managedChannelBuilder.build();
        }
        return new Target_io_opentelemetry_exporter_otlp_trace_OtlpGrpcSpanExporter(channel, timeoutNanos);
    }

}

@TargetClass(className = "io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter")
final class Target_io_opentelemetry_exporter_otlp_trace_OtlpGrpcSpanExporter {
    // Just provide access to "OtlpGrpcSpanExporter"

    @Alias
    public Target_io_opentelemetry_exporter_otlp_trace_OtlpGrpcSpanExporter(ManagedChannel channel, long timeoutNanos) {

    }
}

class OtlpExporterSubstitutions {
}
