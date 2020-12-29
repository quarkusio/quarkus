package io.quarkus.grpc.runtime.supports;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
import static io.grpc.netty.NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.spi.CreationalContext;
import javax.net.ssl.SSLException;

import org.jboss.logging.Logger;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.grpc.runtime.GrpcClientInterceptorContainer;
import io.quarkus.grpc.runtime.annotations.GrpcServiceLiteral;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;

@SuppressWarnings({ "OptionalIsPresent", "Convert2Lambda" })
public class Channels {

    private static final Logger LOGGER = Logger.getLogger(Channels.class.getName());
    private static final String SERVERS_DELIMITER = ",";
    private static final String PORT_DELIMITER = ":";

    private Channels() {
        // Avoid direct instantiation
    }

    public static Channel createChannel(String name) throws SSLException {
        InstanceHandle<GrpcClientConfigProvider> instance = Arc.container().instance(GrpcClientConfigProvider.class);

        if (!instance.isAvailable()) {
            throw new IllegalStateException("Unable to find the GrpcClientConfigProvider");
        }

        GrpcClientConfiguration config = instance.get().getConfiguration(name);
        final Optional<String> servers = config.servers;
        final Optional<String> host = config.host;
        final Optional<Integer> port = config.port;
        String target;
        if (servers.isPresent()) {
            target = validateServers(
                    config.servers.orElseThrow(() -> new IllegalArgumentException("Unable to build server destination.")));

        } else if (host.isPresent() && port.isPresent()) {
            target = host.get() + ":" + host.get();
        } else {
            throw new IllegalArgumentException("Either provide a list of servers or a valid host and port");
        }

        boolean plainText = !config.ssl.trustStore.isPresent();
        Optional<Boolean> usePlainText = config.plainText;
        if (usePlainText.isPresent()) {
            plainText = usePlainText.get();
        }

        SslContext context = null;
        if (!plainText) {
            Path trustStorePath = config.ssl.trustStore.orElse(null);
            Path certificatePath = config.ssl.certificate.orElse(null);
            Path keyPath = config.ssl.key.orElse(null);
            SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();
            if (trustStorePath != null) {
                try (InputStream stream = streamFor(trustStorePath, "trust store")) {
                    sslContextBuilder.trustManager(stream);
                } catch (IOException e) {
                    throw new UncheckedIOException("Configuring gRPC client trust store failed", e);
                }
            }

            if (certificatePath != null && keyPath != null) {
                try (InputStream certificate = streamFor(certificatePath, "certificate");
                        InputStream key = streamFor(keyPath, "key")) {
                    sslContextBuilder.keyManager(certificate, key);
                } catch (IOException e) {
                    throw new UncheckedIOException("Configuring gRPC client certificate failed", e);
                }
            }

            context = sslContextBuilder.build();
        }

        NettyChannelBuilder builder = NettyChannelBuilder.forTarget(target)
                .flowControlWindow(config.flowControlWindow.orElse(DEFAULT_FLOW_CONTROL_WINDOW))
                .keepAliveWithoutCalls(config.keepAliveWithoutCalls)
                .maxHedgedAttempts(config.maxHedgedAttempts)
                .maxRetryAttempts(config.maxRetryAttempts)
                .maxInboundMetadataSize(config.maxInboundMetadataSize.orElse(DEFAULT_MAX_HEADER_LIST_SIZE))
                .maxInboundMessageSize(config.maxInboundMessageSize.orElse(DEFAULT_MAX_MESSAGE_SIZE))
                .negotiationType(NegotiationType.valueOf(config.negotiationType.toUpperCase()));

        if (config.loadBalancer.isPresent()) {
            builder.defaultLoadBalancingPolicy(config.loadBalancer.get());
        }

        if (config.retry) {
            builder.enableRetry();
        } else {
            builder.disableRetry();
        }

        if (config.maxTraceEvents.isPresent()) {
            builder.maxTraceEvents(config.maxTraceEvents.getAsInt());
        }
        Optional<String> userAgent = config.userAgent;
        if (userAgent.isPresent()) {
            builder.userAgent(userAgent.get());
        }
        if (config.retryBufferSize.isPresent()) {
            builder.retryBufferSize(config.retryBufferSize.getAsLong());
        }
        if (config.perRpcBufferLimit.isPresent()) {
            builder.perRpcBufferLimit(config.perRpcBufferLimit.getAsLong());
        }
        Optional<String> overrideAuthority = config.overrideAuthority;
        if (overrideAuthority.isPresent()) {
            builder.overrideAuthority(overrideAuthority.get());
        }
        Optional<Duration> keepAliveTime = config.keepAliveTime;
        if (keepAliveTime.isPresent()) {
            builder.keepAliveTime(keepAliveTime.get().toMillis(), TimeUnit.MILLISECONDS);
        }
        Optional<Duration> keepAliveTimeout = config.keepAliveTimeout;
        if (keepAliveTimeout.isPresent()) {
            builder.keepAliveTimeout(keepAliveTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
        }
        Optional<Duration> idleTimeout = config.idleTimeout;
        if (idleTimeout.isPresent()) {
            builder.keepAliveTimeout(idleTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
        }

        if (plainText) {
            builder.usePlaintext();
        }
        if (context != null) {
            builder.sslContext(context);
        }

        // Client-side interceptors
        InstanceHandle<GrpcClientInterceptorContainer> interceptors = Arc.container()
                .instance(GrpcClientInterceptorContainer.class);
        for (ClientInterceptor clientInterceptor : interceptors.get().getSortedInterceptors()) {
            builder.intercept(clientInterceptor);
        }

        return builder.build();
    }

    /**
     * Validates a {@link #SERVERS_DELIMITER} delimited list of servers
     *
     * @param flatServers flat list of servers being validated
     * @return the same provided list if valid
     */
    private static String validateServers(final String flatServers) {

        final String[] servers = flatServers.split(SERVERS_DELIMITER);
        for (final String server : servers) {
            if (server.split(PORT_DELIMITER).length != 2) {
                throw new IllegalArgumentException("Invalid server configuration: " + server);
            }
        }

        return flatServers;
    }

    private static InputStream streamFor(Path path, String resourceName) {
        final InputStream resource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(path.toString());
        if (resource != null) {
            return resource;
        } else {
            try {
                return Files.newInputStream(path);
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to read " + resourceName + " from " + path, e);
            }
        }
    }

    public static Channel retrieveChannel(String name) {
        InstanceHandle<Channel> instance = Arc.container().instance(Channel.class, GrpcServiceLiteral.of(name));
        if (!instance.isAvailable()) {
            throw new IllegalStateException("Unable to retrieve the gRPC Channel " + name);
        }

        return instance.get();
    }

    public static class ChannelDestroyer implements BeanDestroyer<Channel> {

        @Override
        public void destroy(Channel instance, CreationalContext creationalContext, Map params) {
            if (instance instanceof ManagedChannel) {
                ManagedChannel channel = (ManagedChannel) instance;
                LOGGER.info("Shutting down gRPC channel " + channel);
                channel.shutdownNow();
                try {
                    channel.awaitTermination(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LOGGER.info("Unable to shutdown channel after 10 seconds");
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
