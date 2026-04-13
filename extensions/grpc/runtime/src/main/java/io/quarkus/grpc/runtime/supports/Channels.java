package io.quarkus.grpc.runtime.supports;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
import static io.quarkus.grpc.runtime.supports.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.grpc.runtime.supports.SSLConfigHelper.configureJksTrustOptions;
import static io.quarkus.grpc.runtime.supports.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.grpc.runtime.supports.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.grpc.runtime.supports.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.grpc.runtime.supports.SSLConfigHelper.configurePfxTrustOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.util.TypeLiteral;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.RegisterClientInterceptor;
import io.quarkus.grpc.api.ChannelBuilderCustomizer;
import io.quarkus.grpc.runtime.ClientInterceptorStorage;
import io.quarkus.grpc.runtime.GrpcClientInterceptorContainer;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.stork.StorkGrpcChannel;
import io.quarkus.grpc.runtime.stork.VertxStorkMeasuringGrpcInterceptor;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.stork.Stork;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClientOptions;
import io.vertx.grpcio.client.GrpcIoClient;
import io.vertx.grpcio.client.GrpcIoClientChannel;

@SuppressWarnings({ "OptionalIsPresent" })
public class Channels {

    private static final Logger LOGGER = Logger.getLogger(Channels.class.getName());

    private Channels() {
        // Avoid direct instantiation
    }

    @SuppressWarnings("rawtypes")
    public static Channel createChannel(String name, Set<String> perClientInterceptors) throws Exception {
        ArcContainer container = Arc.container();

        InstanceHandle<GrpcClientConfigProvider> instance = container.instance(GrpcClientConfigProvider.class);
        if (!instance.isAvailable()) {
            throw new IllegalStateException("Unable to find the GrpcClientConfigProvider");
        }
        instance.get();

        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        GrpcConfiguration grpcConfig = config.getConfigMapping(GrpcConfiguration.class);
        GrpcClientConfiguration clientConfig = grpcConfig.clients().get(name);

        String host = clientConfig.host();
        int port = clientConfig.port();
        if (LaunchMode.current() == LaunchMode.TEST) {
            if (clientConfig.testPort().isEmpty()) {
                if (clientConfig.tlsConfigurationName().isPresent() || clientConfig.tls().enabled()
                        || (clientConfig.plainText().isPresent() && !clientConfig.plainText().get())) {
                    port = 8444;
                } else {
                    port = 8081;
                }
            } else {
                port = clientConfig.testPort().getAsInt();
            }
        }

        String nameResolver = clientConfig.nameResolver();

        boolean stork = Stork.STORK.equalsIgnoreCase(nameResolver);

        String[] resolverSplit = nameResolver.split(":");
        String resolver = resolverSplit[0];

        // Client-side interceptors
        GrpcClientInterceptorContainer interceptorContainer = container
                .instance(GrpcClientInterceptorContainer.class).get();
        if (stork) {
            perClientInterceptors = new HashSet<>(perClientInterceptors);
            perClientInterceptors.add(VertxStorkMeasuringGrpcInterceptor.class.getName());
        }

        List<ChannelBuilderCustomizer<?>> channelBuilderCustomizers = container
                .select(new TypeLiteral<ChannelBuilderCustomizer<?>>() {
                }, Any.Literal.INSTANCE)
                .stream()
                .sorted(Comparator.<ChannelBuilderCustomizer<?>, Integer> comparing(ChannelBuilderCustomizer::priority))
                .toList();

        // Look whether the client use plain text
        // If set -> use the configured value
        // Otherwise -> if tls is enabled ot tls-configuration-name is set -> true
        boolean plainText;
        if (clientConfig.plainText().isPresent()) {
            plainText = clientConfig.plainText().get();
        } else {
            plainText = !clientConfig.tls().enabled() && clientConfig.tlsConfigurationName().isEmpty();
        }

        HttpClientOptions options = new HttpClientOptions();
        options.setHttp2ClearTextUpgrade(false); // this fixes i30379

        // Start with almost empty options and default max msg size ...
        GrpcClientOptions clientOptions = new GrpcClientOptions()
                .setMaxMessageSize(clientConfig.maxInboundMessageSize().orElse(DEFAULT_MAX_MESSAGE_SIZE));

        for (ChannelBuilderCustomizer customizer : channelBuilderCustomizers) {
            customizer.customize(name, clientConfig, clientOptions);
        }

        if (!plainText) {
            TlsConfigurationRegistry registry = Arc.container().select(TlsConfigurationRegistry.class).get();

            // always set ssl + alpn for plain-text=false
            options.setSsl(true);
            options.setUseAlpn(true);

            TlsConfiguration configuration = null;
            if (clientConfig.tlsConfigurationName().isPresent()) {
                Optional<TlsConfiguration> maybeConfiguration = registry.get(clientConfig.tlsConfigurationName().get());
                if (!maybeConfiguration.isPresent()) {
                    throw new IllegalStateException("Unable to find the TLS configuration "
                            + clientConfig.tlsConfigurationName().get() + " for the gRPC client " + name + ".");
                }
                configuration = maybeConfiguration.get();
            } else if (registry.getDefault().isPresent() && (registry.getDefault().get().getTrustStoreOptions() != null
                    || registry.getDefault().get().isTrustAll())) {
                configuration = registry.getDefault().get();
            }

            if (configuration != null) {
                TlsConfigUtils.configure(options, configuration);
            } else if (clientConfig.tls().enabled()) {
                GrpcClientConfiguration.TlsClientConfig tls = clientConfig.tls();
                options.setSsl(true).setTrustAll(tls.trustAll());

                configurePemTrustOptions(options, tls.trustCertificatePem());
                configureJksTrustOptions(options, tls.trustCertificateJks());
                configurePfxTrustOptions(options, tls.trustCertificateP12());

                configurePemKeyCertOptions(options, tls.keyCertificatePem());
                configureJksKeyCertOptions(options, tls.keyCertificateJks());
                configurePfxKeyCertOptions(options, tls.keyCertificateP12());
                options.setVerifyHost(tls.verifyHostname());
            }
        }

        options.setKeepAlive(clientConfig.keepAliveWithoutCalls());
        Optional<Duration> keepAliveTimeout = clientConfig.keepAliveTimeout();
        if (keepAliveTimeout.isPresent()) {
            int keepAliveTimeoutN = (int) keepAliveTimeout.get().toSeconds();
            options.setKeepAliveTimeout(keepAliveTimeoutN);
            options.setHttp2KeepAliveTimeout(keepAliveTimeoutN);
        }
        Optional<Duration> idleTimeout = clientConfig.idleTimeout();
        if (idleTimeout.isPresent()) {
            options.setIdleTimeout((int) idleTimeout.get().toMillis());
            options.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
        }

        // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with grpc.<name>.
        // See io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractPrefix and
        // io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractClientName
        options.setMetricsName("grpc|" + name);

        Vertx vertx = container.instance(Vertx.class).get();
        GrpcIoClient client = GrpcIoClient.client(
                vertx,
                clientOptions,
                options);

        Channel channel;
        if (stork) {
            ManagedExecutor executor = container.instance(ManagedExecutor.class).get();
            channel = new StorkGrpcChannel(client, clientConfig.host(), clientConfig.stork(), executor); // host = service-name
        } else {
            channel = new GrpcIoClientChannel(client, SocketAddress.inetSocketAddress(port, host));
        }
        LOGGER.debugf("Target for client '%s': %s", name, host + ":" + port);

        List<ClientInterceptor> interceptors = new ArrayList<>();
        interceptors.addAll(interceptorContainer.getSortedPerServiceInterceptors(perClientInterceptors));
        interceptors.addAll(interceptorContainer.getSortedGlobalInterceptors());

        LOGGER.debug("Creating Vert.x gRPC channel ...");

        return new InternalGrpcChannel(client, channel, ClientInterceptors.intercept(channel, interceptors));

    }

    private static Buffer bufferFor(Path path, String resourceName) throws IOException {
        try (InputStream stream = streamFor(path, resourceName)) {
            return Buffer.buffer(stream.readAllBytes());
        }
    }

    private static InputStream streamFor(Path path, String resourceName) {
        final InputStream resource = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(ClassPathUtils.toResourceName(path));
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

    @SuppressWarnings("unchecked")
    public static Channel retrieveChannel(String name, Set<String> perClientInterceptors) {
        ClientInterceptorStorage clientInterceptorStorage = Arc.container().instance(ClientInterceptorStorage.class).get();
        Annotation[] qualifiers = new Annotation[perClientInterceptors.size() + 1];
        int idx = 0;
        qualifiers[idx++] = GrpcClient.Literal.of(name);
        for (String interceptor : perClientInterceptors) {
            qualifiers[idx++] = RegisterClientInterceptor.Literal
                    .of((Class<? extends ClientInterceptor>) clientInterceptorStorage.getPerClientInterceptor(interceptor));
        }
        InstanceHandle<Channel> instance = Arc.container().instance(Channel.class, qualifiers);
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
            } else if (instance instanceof InternalGrpcChannel) {
                InternalGrpcChannel channel = (InternalGrpcChannel) instance;
                Channel original = channel.original;
                LOGGER.info("Shutting down Vert.x gRPC channel " + original);
                try {
                    if (original instanceof StorkGrpcChannel) {
                        ((StorkGrpcChannel) original).close();
                    }
                    channel.client.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
                } catch (ExecutionException | TimeoutException e) {
                    LOGGER.warn("Unable to shutdown channel after 10 seconds", e);
                } catch (InterruptedException e) {
                    LOGGER.info("Unable to shutdown channel after 10 seconds");
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static class InternalGrpcChannel extends Channel {
        private final io.vertx.grpc.client.GrpcClient client;
        private final Channel original;
        private final Channel delegate;

        public InternalGrpcChannel(io.vertx.grpc.client.GrpcClient client, Channel original, Channel delegate) {
            this.client = client;
            this.original = original;
            this.delegate = delegate;
        }

        @Override
        public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(
                MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
            return delegate.newCall(methodDescriptor, callOptions);
        }

        @Override
        public String authority() {
            return delegate.authority();
        }
    }
}
