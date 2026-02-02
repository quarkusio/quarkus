package io.quarkus.grpc.runtime.supports;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
import static io.grpc.netty.NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW;
import static io.quarkus.grpc.runtime.config.GrpcClientConfiguration.DNS;
import static io.quarkus.grpc.runtime.supports.SSLConfigHelper.*;

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
import java.util.LinkedHashMap;
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
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.RegisterClientInterceptor;
import io.quarkus.grpc.api.ChannelBuilderCustomizer;
import io.quarkus.grpc.runtime.ClientInterceptorStorage;
import io.quarkus.grpc.runtime.GrpcClientInterceptorContainer;
import io.quarkus.grpc.runtime.GrpcServer;
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.runtime.stork.StorkGrpcChannel;
import io.quarkus.grpc.runtime.stork.StorkMeasuringGrpcInterceptor;
import io.quarkus.grpc.runtime.stork.VertxStorkMeasuringGrpcInterceptor;
import io.quarkus.grpc.spi.GrpcBuilderProvider;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.quarkus.value.registry.ValueRegistry;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.stork.Stork;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClientChannel;
import io.vertx.grpc.client.GrpcClientOptions;

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

        ValueRegistry valueRegistry = container.instance(ValueRegistry.class).get();
        GrpcServer grpcServer = valueRegistry.get(GrpcServer.GRPC_SERVER);
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        GrpcConfiguration grpcConfig = config.getConfigMapping(GrpcConfiguration.class);
        GrpcClientConfiguration clientConfig = grpcConfig.clients().get(name);
        GrpcServerConfiguration serverConfig = grpcConfig.server();

        GrpcBuilderProvider provider = GrpcBuilderProvider.findChannelBuilderProvider(clientConfig);

        boolean vertxGrpc = clientConfig.useQuarkusGrpcClient();

        String host = clientConfig.host();
        int port = clientConfig.port();
        if (LaunchMode.current() == LaunchMode.TEST) {
            port = clientConfig.testPort().orElseGet(() -> {
                LOGGER.info("LaunchMode TEST detected. Overriding existing port configuration for gRPC client/server. "
                        + "Set quarkus.grpc.clients.\"client-name\".test-port to configure the test port");
                return grpcServer.getPort();
            });

            if (port == -1) {
                // In same cases, a Channel may be created without the port being assigned
                port = serverConfig.testPort();
            }

            if (!grpcConfig.clients().containsKey(name)
                    && (serverConfig.ssl().certificate().isPresent() || serverConfig.ssl().keyStore().isPresent())) {
                LOGGER.warn("gRPC client created without configuration and the gRPC server is configured for SSL. " +
                        "Configuring SSL for such clients is not supported.");
            }
        }

        String nameResolver = clientConfig.nameResolver();

        boolean stork = Stork.STORK.equalsIgnoreCase(nameResolver);

        String[] resolverSplit = nameResolver.split(":");
        String resolver = provider != null ? provider.resolver() : resolverSplit[0];

        // TODO -- does this work for Vert.x gRPC client?
        if (provider != null) {
            host = provider.adjustHost(host);
        } else if (!vertxGrpc && DNS.equalsIgnoreCase(resolver)) {
            host = "/" + host; // dns or xds name resolver needs triple slash at the beginning
        }

        // Client-side interceptors
        GrpcClientInterceptorContainer interceptorContainer = container
                .instance(GrpcClientInterceptorContainer.class).get();
        if (stork) {
            perClientInterceptors = new HashSet<>(perClientInterceptors);
            if (vertxGrpc) {
                perClientInterceptors.add(VertxStorkMeasuringGrpcInterceptor.class.getName());
            } else {
                perClientInterceptors.add(StorkMeasuringGrpcInterceptor.class.getName());
            }
        }

        List<ChannelBuilderCustomizer<?>> channelBuilderCustomizers = container
                .select(new TypeLiteral<ChannelBuilderCustomizer<?>>() {
                }, Any.Literal.INSTANCE)
                .stream()
                .sorted(Comparator.<ChannelBuilderCustomizer<?>, Integer> comparing(ChannelBuilderCustomizer::priority))
                .toList();

        boolean plainText = clientConfig.ssl().trustStore().isEmpty();
        Optional<Boolean> usePlainText = clientConfig.plainText();
        if (usePlainText.isPresent()) {
            plainText = usePlainText.get();
        }

        if (!vertxGrpc) {
            String target = String.format("%s://%s:%d", resolver, host, port);
            LOGGER.debugf("Target for client '%s': %s", name, target);

            SslContext context = null;
            if (!plainText && provider == null) {
                Path trustStorePath = clientConfig.ssl().trustStore().orElse(null);
                Path certificatePath = clientConfig.ssl().certificate().orElse(null);
                Path keyPath = clientConfig.ssl().key().orElse(null);
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

            String loadBalancingPolicy = stork ? Stork.STORK : clientConfig.loadBalancingPolicy();

            ManagedChannelBuilder<?> builder;
            if (provider != null) {
                builder = provider.createChannelBuilder(clientConfig, target);
            } else {
                builder = NettyChannelBuilder.forTarget(target);
            }

            Map<String, Object> configMap = new LinkedHashMap<>();
            for (ChannelBuilderCustomizer customizer : channelBuilderCustomizers) {
                Map<String, Object> map = customizer.customize(name, clientConfig, builder);
                configMap.putAll(map);
            }
            builder.defaultServiceConfig(configMap);

            if (clientConfig.useVertxEventLoop() && builder instanceof NettyChannelBuilder) {
                NettyChannelBuilder ncBuilder = (NettyChannelBuilder) builder;
                // just use the existing Vertx event loop group, if possible
                Vertx vertx = container.instance(Vertx.class).get();
                // only support NIO for now, since Vertx::transport is not exposed in the API
                if (vertx != null && !vertx.isNativeTransportEnabled()) {
                    // see https://github.com/eclipse-vertx/vert.x/pull/5292
                    boolean reuseNettyAllocators = Boolean.getBoolean("vertx.reuseNettyAllocators");
                    if (reuseNettyAllocators) {
                        // let Netty Grpc to re-use the default Netty allocator as well
                        System.setProperty("io.grpc.netty.useCustomAllocator", "false");
                    }
                    ncBuilder = ncBuilder.eventLoopGroup(vertx.nettyEventLoopGroup())
                            .channelType(NioSocketChannel.class);
                }
                builder = ncBuilder
                        // clients are intercepted using the IOThreadClientInterceptor interceptor which will decide on which
                        // thread the messages should be processed.
                        .directExecutor() // will use I/O thread - must not be blocked.
                        .offloadExecutor(Infrastructure.getDefaultExecutor())
                        .defaultLoadBalancingPolicy(loadBalancingPolicy)
                        .flowControlWindow(clientConfig.flowControlWindow().orElse(DEFAULT_FLOW_CONTROL_WINDOW))
                        .keepAliveWithoutCalls(clientConfig.keepAliveWithoutCalls())
                        .maxHedgedAttempts(clientConfig.maxHedgedAttempts())
                        .maxRetryAttempts(clientConfig.maxRetryAttempts())
                        .maxInboundMetadataSize(clientConfig.maxInboundMetadataSize().orElse(DEFAULT_MAX_HEADER_LIST_SIZE))
                        .maxInboundMessageSize(clientConfig.maxInboundMessageSize().orElse(DEFAULT_MAX_MESSAGE_SIZE))
                        .negotiationType(NegotiationType.valueOf(clientConfig.negotiationType().toUpperCase()));

                if (context != null) {
                    ncBuilder.sslContext(context);
                }
            }

            if (clientConfig.retry()) {
                builder.enableRetry();
            } else {
                builder.disableRetry();
            }

            if (clientConfig.maxTraceEvents().isPresent()) {
                builder.maxTraceEvents(clientConfig.maxTraceEvents().getAsInt());
            }
            Optional<String> userAgent = clientConfig.userAgent();
            if (userAgent.isPresent()) {
                builder.userAgent(userAgent.get());
            }
            if (clientConfig.retryBufferSize().isPresent()) {
                builder.retryBufferSize(clientConfig.retryBufferSize().getAsLong());
            }
            if (clientConfig.perRpcBufferLimit().isPresent()) {
                builder.perRpcBufferLimit(clientConfig.perRpcBufferLimit().getAsLong());
            }
            Optional<String> overrideAuthority = clientConfig.overrideAuthority();
            if (overrideAuthority.isPresent()) {
                builder.overrideAuthority(overrideAuthority.get());
            }
            Optional<Duration> keepAliveTime = clientConfig.keepAliveTime();
            if (keepAliveTime.isPresent()) {
                builder.keepAliveTime(keepAliveTime.get().toMillis(), TimeUnit.MILLISECONDS);
            }
            Optional<Duration> keepAliveTimeout = clientConfig.keepAliveTimeout();
            if (keepAliveTimeout.isPresent()) {
                builder.keepAliveTimeout(keepAliveTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
            }
            Optional<Duration> idleTimeout = clientConfig.idleTimeout();
            if (idleTimeout.isPresent()) {
                builder.idleTimeout(idleTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
            }

            if (plainText && provider == null) {
                builder.usePlaintext();
            }

            interceptorContainer.getSortedPerServiceInterceptors(perClientInterceptors).forEach(builder::intercept);
            interceptorContainer.getSortedGlobalInterceptors().forEach(builder::intercept);

            LOGGER.info(String.format("Creating %s gRPC channel ...",
                    provider != null ? provider.channelInfo(clientConfig) : "Netty"));

            return builder.build();
        } else {
            // Vert.x client
            HttpClientOptions options = new HttpClientOptions();
            options.setHttp2ClearTextUpgrade(false); // this fixes i30379

            // Start with almost empty options and default max msg size ...
            GrpcClientOptions clientOptions = new GrpcClientOptions()
                    .setTransportOptions(options)
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
                } else {
                    if (clientConfig.ssl().trustStore().isPresent()) {
                        Optional<Path> trustStorePath = clientConfig.ssl().trustStore();
                        PemTrustOptions to = new PemTrustOptions();
                        to.addCertValue(bufferFor(trustStorePath.get(), "trust store"));
                        options.setTrustOptions(to);
                        Optional<Path> certificatePath = clientConfig.ssl().certificate();
                        Optional<Path> keyPath = clientConfig.ssl().key();
                        if (certificatePath.isPresent() && keyPath.isPresent()) {
                            PemKeyCertOptions cko = new PemKeyCertOptions();
                            cko.setCertValue(bufferFor(certificatePath.get(), "certificate"));
                            cko.setKeyValue(bufferFor(keyPath.get(), "key"));
                            options.setKeyCertOptions(cko);
                        }
                    }
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
            io.vertx.grpc.client.GrpcClient client = io.vertx.grpc.client.GrpcClient.client(
                    vertx,
                    clientOptions);

            Channel channel;
            if (stork) {
                ManagedExecutor executor = container.instance(ManagedExecutor.class).get();
                channel = new StorkGrpcChannel(client, clientConfig.host(), clientConfig.stork(), executor); // host = service-name
            } else {
                channel = new GrpcClientChannel(client, SocketAddress.inetSocketAddress(port, host));
            }
            LOGGER.debugf("Target for client '%s': %s", name, host + ":" + port);

            List<ClientInterceptor> interceptors = new ArrayList<>();
            interceptors.addAll(interceptorContainer.getSortedPerServiceInterceptors(perClientInterceptors));
            interceptors.addAll(interceptorContainer.getSortedGlobalInterceptors());

            LOGGER.debug("Creating Vert.x gRPC channel ...");

            return new InternalGrpcChannel(client, channel, ClientInterceptors.intercept(channel, interceptors));
        }
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
