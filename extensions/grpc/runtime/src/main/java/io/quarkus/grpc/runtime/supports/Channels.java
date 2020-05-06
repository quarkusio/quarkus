package io.quarkus.grpc.runtime.supports;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
import static io.grpc.netty.NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Prioritized;
import javax.net.ssl.SSLException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.grpc.runtime.annotations.GrpcServiceLiteral;

public class Channels {

    private Channels() {
        // Avoid direct instantiation
    }

    public static Channel createChannel(String prefix) throws SSLException {
        Config config = ConfigProvider.getConfig();
        String host = getMandatoryProperty(config, prefix, "host", String.class);
        int port = getOptionalProperty(config, prefix, "port", Integer.class, 9000);
        boolean defaultPlainText = getOptionalProperty(config, prefix, "ssl.trust-store", String.class, null) == null;
        boolean plainText = getOptionalProperty(config, prefix, "plain-text", Boolean.class, defaultPlainText);

        SslContext context = null;
        if (!plainText) {
            String trustStorePath = getOptionalProperty(config, prefix, "ssl.trust-store", String.class, null);
            String certificatePath = getOptionalProperty(config, prefix, "ssl.certificate", String.class, null);
            String keyPath = getOptionalProperty(config, prefix, "ssl.key", String.class, null);
            SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();
            if (trustStorePath != null) {
                sslContextBuilder.trustManager(new File(trustStorePath));
            }

            if (certificatePath != null) {
                sslContextBuilder.keyManager(new File(certificatePath), new File(keyPath));
            }

            context = sslContextBuilder.build();
        }

        Optional<Duration> keepAliveTime = getOptionalProperty(config, prefix, "keep-alive-time", Duration.class);
        Optional<Duration> keepAliveTimeout = getOptionalProperty(config, prefix, "keep-alive-timeout", Duration.class);
        Optional<Duration> idleTimeout = getOptionalProperty(config, prefix, "idle-timeout", Duration.class);

        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(host, port)
                .flowControlWindow(getOptionalProperty(config, prefix, "flow-control-window", Integer.class,
                        DEFAULT_FLOW_CONTROL_WINDOW))
                .keepAliveWithoutCalls(
                        getOptionalProperty(config, prefix, "keep-alive-without-calls", Boolean.class, false))
                .maxHedgedAttempts(getOptionalProperty(config, prefix, "max-hedged-attempts", Integer.TYPE, 5))
                .maxRetryAttempts(getOptionalProperty(config, prefix, "max-retry-attempts", Integer.TYPE, 5))
                .maxInboundMetadataSize(getOptionalProperty(config, prefix, "max-inbound-metadata-size", Integer.TYPE,
                        DEFAULT_MAX_HEADER_LIST_SIZE))
                .maxInboundMessageSize(getOptionalProperty(config, prefix, "max-inbound-message-size", Integer.TYPE,
                        DEFAULT_MAX_MESSAGE_SIZE))
                .negotiationType(NegotiationType.valueOf(
                        getOptionalProperty(config, prefix, "negotiation-type", String.class, "TLS").toUpperCase()));

        getOptionalProperty(config, prefix, "retry", Boolean.class).ifPresent(enabled -> {
            if (enabled) {
                builder.enableRetry();
            } else {
                builder.disableRetry();
            }
        });

        getOptionalProperty(config, prefix, "max-trace-events", Integer.class).ifPresent(builder::maxTraceEvents);
        getOptionalProperty(config, prefix, "user-agent", String.class).ifPresent(builder::userAgent);
        getOptionalProperty(config, prefix, "retry-buffer-size", Long.class).ifPresent(builder::retryBufferSize);
        getOptionalProperty(config, prefix, "pre-rpc-buffer-limit", Long.class).ifPresent(builder::perRpcBufferLimit);
        getOptionalProperty(config, prefix, "override-authority", String.class).ifPresent(builder::overrideAuthority);

        keepAliveTime.ifPresent(duration -> builder.keepAliveTime(duration.toMillis(), TimeUnit.MILLISECONDS));
        keepAliveTimeout.ifPresent(duration -> builder.keepAliveTimeout(duration.toMillis(), TimeUnit.MILLISECONDS));
        idleTimeout.ifPresent(duration -> builder.idleTimeout(duration.toMillis(), TimeUnit.MILLISECONDS));

        if (plainText) {
            builder.usePlaintext();
        }
        if (context != null) {
            builder.sslContext(context);
        }

        // Client-side interceptors
        Instance<ClientInterceptor> interceptors = Arc.container().beanManager().createInstance()
                .select(ClientInterceptor.class);
        getSortedInterceptors(interceptors).forEach(builder::intercept);

        return builder.build();
    }

    public static Channel retrieveChannel(String name) {
        InstanceHandle<Channel> instance = Arc.container().instance(Channel.class, GrpcServiceLiteral.of(name));
        if (!instance.isAvailable()) {
            throw new IllegalStateException("Unable to retrieve the GRPC Channel " + name);
        }
        return instance.get();
    }

    private static <T> T getMandatoryProperty(Config config, String prefix, String attr, Class<T> type) {
        return config.getValue(prefix + attr, type);
    }

    private static <T> T getOptionalProperty(Config config, String prefix, String attr, Class<T> type, T defaultValue) {
        return config.getOptionalValue(prefix + attr, type).orElse(defaultValue);
    }

    private static <T> Optional<T> getOptionalProperty(Config config, String prefix, String attr, Class<T> type) {
        return config.getOptionalValue(prefix + attr, type);
    }

    private static List<ClientInterceptor> getSortedInterceptors(Instance<ClientInterceptor> interceptors) {
        if (interceptors.isUnsatisfied()) {
            return Collections.emptyList();
        }

        return interceptors.stream().sorted((si1, si2) -> {
            int p1 = 0;
            int p2 = 0;
            if (si1 instanceof Prioritized) {
                p1 = ((Prioritized) si1).getPriority();
            }
            if (si2 instanceof Prioritized) {
                p2 = ((Prioritized) si2).getPriority();
            }
            if (si1.equals(si2)) {
                return 0;
            }
            return Integer.compare(p1, p2);
        }).collect(Collectors.toList());
    }

}
