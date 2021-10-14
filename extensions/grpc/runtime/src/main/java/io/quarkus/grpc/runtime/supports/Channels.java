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
import java.util.OptionalInt;
import java.util.OptionalLong;
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
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.runtime.GrpcClientInterceptorContainer;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.runtime.config.SslClientConfig;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.stork.Stork;

@SuppressWarnings({ "OptionalIsPresent", "Convert2Lambda" })
public class Channels {

    private static final Logger LOGGER = Logger.getLogger(Channels.class.getName());

    private Channels() {
        // Avoid direct instantiation
    }

    public static Channel createChannel(String name) throws SSLException {
        InstanceHandle<GrpcClientConfigProvider> instance = Arc.container().instance(GrpcClientConfigProvider.class);

        if (!instance.isAvailable()) {
            throw new IllegalStateException("Unable to find the GrpcClientConfigProvider");
        }

        GrpcClientConfigProvider configProvider = instance.get();
        GrpcClientConfiguration config = configProvider.getConfiguration(name);

        if (config == null && LaunchMode.current() == LaunchMode.TEST) {
            LOGGER.infof(
                    "gRPC client %s created without configuration. We are assuming that it's created to test your gRPC services.",
                    name);
            config = testConfig(configProvider.getServerConfiguration());
        }

        if (config == null) {
            throw new IllegalStateException("gRPC client " + name + " is missing configuration.");
        }

        String host = config.host;
        int port = config.port;
        String nameResolver = config.nameResolver;

        String[] resolverSplit = nameResolver.split(":");

        if (GrpcClientConfiguration.DNS.equalsIgnoreCase(resolverSplit[0])) {
            host = "/" + host; // dns name resolver needs triple slash at the beginning
        }

        String target = String.format("%s://%s:%d", resolverSplit[0], host, port);

        boolean plainText = config.ssl.trustStore.isEmpty();
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

        String loadBalancingPolicy = config.loadBalancingPolicy;

        if (Stork.STORK.equalsIgnoreCase(nameResolver)) {
            loadBalancingPolicy = Stork.STORK;
        }

        NettyChannelBuilder builder = NettyChannelBuilder
                .forTarget(target)
                .defaultLoadBalancingPolicy(loadBalancingPolicy)
                .flowControlWindow(config.flowControlWindow.orElse(DEFAULT_FLOW_CONTROL_WINDOW))
                .keepAliveWithoutCalls(config.keepAliveWithoutCalls)
                .maxHedgedAttempts(config.maxHedgedAttempts)
                .maxRetryAttempts(config.maxRetryAttempts)
                .maxInboundMetadataSize(config.maxInboundMetadataSize.orElse(DEFAULT_MAX_HEADER_LIST_SIZE))
                .maxInboundMessageSize(config.maxInboundMessageSize.orElse(DEFAULT_MAX_MESSAGE_SIZE))
                .negotiationType(NegotiationType.valueOf(config.negotiationType.toUpperCase()));

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

    private static GrpcClientConfiguration testConfig(GrpcServerConfiguration serverConfiguration) {
        GrpcClientConfiguration config = new GrpcClientConfiguration();
        config.port = serverConfiguration.testPort;
        config.host = serverConfiguration.host;
        config.plainText = Optional.of(serverConfiguration.plainText);
        config.compression = Optional.empty();
        config.flowControlWindow = OptionalInt.empty();
        config.idleTimeout = Optional.empty();
        config.keepAliveTime = Optional.empty();
        config.keepAliveTimeout = Optional.empty();
        config.loadBalancingPolicy = "pick_first";
        config.maxHedgedAttempts = 5;
        config.maxInboundMessageSize = OptionalInt.empty();
        config.maxInboundMetadataSize = OptionalInt.empty();
        config.maxRetryAttempts = 0;
        config.maxTraceEvents = OptionalInt.empty();
        config.nameResolver = GrpcClientConfiguration.DNS;
        config.negotiationType = "PLAINTEXT";
        config.overrideAuthority = Optional.empty();
        config.perRpcBufferLimit = OptionalLong.empty();
        config.retry = false;
        config.retryBufferSize = OptionalLong.empty();
        config.ssl = new SslClientConfig();
        config.ssl.key = Optional.empty();
        config.ssl.certificate = Optional.empty();
        config.ssl.trustStore = Optional.empty();
        config.userAgent = Optional.empty();
        if (serverConfiguration.ssl.certificate.isPresent() || serverConfiguration.ssl.keyStore.isPresent()) {
            LOGGER.warn("gRPC client created without configuration and the gRPC server is configured for SSL. " +
                    "Configuring SSL for such clients is not supported.");
        }
        return config;
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
        InstanceHandle<Channel> instance = Arc.container().instance(Channel.class, GrpcClient.Literal.of(name));
        if (!instance.isAvailable()) {
            throw new IllegalStateException("Unable to retrieve the gRPC Channel " + name);
        }

        return instance.get();
    }

    public static class ChannelDestroyer implements BeanDestroyer<Channel> {

        @Override
        public void destroy(Channel instance, CreationalContext<Channel> creationalContext, Map<String, Object> params) {
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
