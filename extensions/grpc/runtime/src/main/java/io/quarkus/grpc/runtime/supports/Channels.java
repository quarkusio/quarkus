package io.quarkus.grpc.runtime.supports;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_HEADER_LIST_SIZE;
import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
import static io.grpc.netty.NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW;
import static io.quarkus.grpc.runtime.GrpcTestPortUtils.testPort;
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
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.util.TypeLiteral;

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
import io.quarkus.grpc.runtime.config.GrpcClientConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.runtime.config.InProcess;
import io.quarkus.grpc.runtime.stork.StorkGrpcChannel;
import io.quarkus.grpc.runtime.stork.StorkMeasuringGrpcInterceptor;
import io.quarkus.grpc.runtime.stork.VertxStorkMeasuringGrpcInterceptor;
import io.quarkus.grpc.spi.GrpcBuilderProvider;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
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

        GrpcBuilderProvider provider = GrpcBuilderProvider.findChannelBuilderProvider(config);

        boolean vertxGrpc = config.useQuarkusGrpcClient();

        String host = config.host();

        // handle client port
        int port = config.port();
        if (LaunchMode.current() == LaunchMode.TEST) {
            port = config.testPort().orElse(testPort(configProvider.getServerConfiguration()));
        }

        String nameResolver = config.nameResolver();

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

        boolean plainText = config.ssl().trustStore().isEmpty();
        Optional<Boolean> usePlainText = config.plainText();
        if (usePlainText.isPresent()) {
            plainText = usePlainText.get();
        }

        if (!vertxGrpc) {
            String target = String.format("%s://%s:%d", resolver, host, port);
            LOGGER.debugf("Target for client '%s': %s", name, target);

            SslContext context = null;
            if (!plainText && provider == null) {
                Path trustStorePath = config.ssl().trustStore().orElse(null);
                Path certificatePath = config.ssl().certificate().orElse(null);
                Path keyPath = config.ssl().key().orElse(null);
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

            String loadBalancingPolicy = stork ? Stork.STORK : config.loadBalancingPolicy();

            ManagedChannelBuilder<?> builder;
            if (provider != null) {
                builder = provider.createChannelBuilder(config, target);
            } else {
                builder = NettyChannelBuilder.forTarget(target);
            }

            Map<String, Object> configMap = new LinkedHashMap<>();
            for (ChannelBuilderCustomizer customizer : channelBuilderCustomizers) {
                Map<String, Object> map = customizer.customize(name, config, builder);
                configMap.putAll(map);
            }
            builder.defaultServiceConfig(configMap);

            if (config.useVertxEventLoop() && builder instanceof NettyChannelBuilder) {
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
                        .flowControlWindow(config.flowControlWindow().orElse(DEFAULT_FLOW_CONTROL_WINDOW))
                        .keepAliveWithoutCalls(config.keepAliveWithoutCalls())
                        .maxHedgedAttempts(config.maxHedgedAttempts())
                        .maxRetryAttempts(config.maxRetryAttempts())
                        .maxInboundMetadataSize(config.maxInboundMetadataSize().orElse(DEFAULT_MAX_HEADER_LIST_SIZE))
                        .maxInboundMessageSize(config.maxInboundMessageSize().orElse(DEFAULT_MAX_MESSAGE_SIZE))
                        .negotiationType(NegotiationType.valueOf(config.negotiationType().toUpperCase()));

                if (context != null) {
                    ncBuilder.sslContext(context);
                }
            }

            if (config.retry()) {
                builder.enableRetry();
            } else {
                builder.disableRetry();
            }

            if (config.maxTraceEvents().isPresent()) {
                builder.maxTraceEvents(config.maxTraceEvents().getAsInt());
            }
            Optional<String> userAgent = config.userAgent();
            if (userAgent.isPresent()) {
                builder.userAgent(userAgent.get());
            }
            if (config.retryBufferSize().isPresent()) {
                builder.retryBufferSize(config.retryBufferSize().getAsLong());
            }
            if (config.perRpcBufferLimit().isPresent()) {
                builder.perRpcBufferLimit(config.perRpcBufferLimit().getAsLong());
            }
            Optional<String> overrideAuthority = config.overrideAuthority();
            if (overrideAuthority.isPresent()) {
                builder.overrideAuthority(overrideAuthority.get());
            }
            Optional<Duration> keepAliveTime = config.keepAliveTime();
            if (keepAliveTime.isPresent()) {
                builder.keepAliveTime(keepAliveTime.get().toMillis(), TimeUnit.MILLISECONDS);
            }
            Optional<Duration> keepAliveTimeout = config.keepAliveTimeout();
            if (keepAliveTimeout.isPresent()) {
                builder.keepAliveTimeout(keepAliveTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
            }
            Optional<Duration> idleTimeout = config.idleTimeout();
            if (idleTimeout.isPresent()) {
                builder.idleTimeout(idleTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
            }

            if (plainText && provider == null) {
                builder.usePlaintext();
            }

            interceptorContainer.getSortedPerServiceInterceptors(perClientInterceptors).forEach(builder::intercept);
            interceptorContainer.getSortedGlobalInterceptors().forEach(builder::intercept);

            LOGGER.info(String.format("Creating %s gRPC channel ...",
                    provider != null ? provider.channelInfo(config) : "Netty"));

            return builder.build();
        } else {
            // Vert.x client
            HttpClientOptions options = new HttpClientOptions();
            options.setHttp2ClearTextUpgrade(false); // this fixes i30379

            // Start with almost empty options and default max msg size ...
            GrpcClientOptions clientOptions = new GrpcClientOptions()
                    .setTransportOptions(options)
                    .setMaxMessageSize(config.maxInboundMessageSize().orElse(DEFAULT_MAX_MESSAGE_SIZE));

            for (ChannelBuilderCustomizer customizer : channelBuilderCustomizers) {
                customizer.customize(name, config, clientOptions);
            }

            if (!plainText) {
                TlsConfigurationRegistry registry = Arc.container().select(TlsConfigurationRegistry.class).get();

                // always set ssl + alpn for plain-text=false
                options.setSsl(true);
                options.setUseAlpn(true);

                TlsConfiguration configuration = null;
                if (config.tlsConfigurationName().isPresent()) {
                    Optional<TlsConfiguration> maybeConfiguration = registry.get(config.tlsConfigurationName().get());
                    if (!maybeConfiguration.isPresent()) {
                        throw new IllegalStateException("Unable to find the TLS configuration "
                                + config.tlsConfigurationName().get() + " for the gRPC client " + name + ".");
                    }
                    configuration = maybeConfiguration.get();
                } else if (registry.getDefault().isPresent() && (registry.getDefault().get().getTrustStoreOptions() != null
                        || registry.getDefault().get().isTrustAll())) {
                    configuration = registry.getDefault().get();
                }

                if (configuration != null) {
                    TlsConfigUtils.configure(options, configuration);
                } else if (config.tls().enabled()) {
                    GrpcClientConfiguration.TlsClientConfig tls = config.tls();
                    options.setSsl(true).setTrustAll(tls.trustAll());

                    configurePemTrustOptions(options, tls.trustCertificatePem());
                    configureJksTrustOptions(options, tls.trustCertificateJks());
                    configurePfxTrustOptions(options, tls.trustCertificateP12());

                    configurePemKeyCertOptions(options, tls.keyCertificatePem());
                    configureJksKeyCertOptions(options, tls.keyCertificateJks());
                    configurePfxKeyCertOptions(options, tls.keyCertificateP12());
                    options.setVerifyHost(tls.verifyHostname());
                } else {
                    if (config.ssl().trustStore().isPresent()) {
                        Optional<Path> trustStorePath = config.ssl().trustStore();
                        PemTrustOptions to = new PemTrustOptions();
                        to.addCertValue(bufferFor(trustStorePath.get(), "trust store"));
                        options.setTrustOptions(to);
                        Optional<Path> certificatePath = config.ssl().certificate();
                        Optional<Path> keyPath = config.ssl().key();
                        if (certificatePath.isPresent() && keyPath.isPresent()) {
                            PemKeyCertOptions cko = new PemKeyCertOptions();
                            cko.setCertValue(bufferFor(certificatePath.get(), "certificate"));
                            cko.setKeyValue(bufferFor(keyPath.get(), "key"));
                            options.setKeyCertOptions(cko);
                        }
                    }
                }
            }

            options.setKeepAlive(config.keepAliveWithoutCalls());
            Optional<Duration> keepAliveTimeout = config.keepAliveTimeout();
            if (keepAliveTimeout.isPresent()) {
                int keepAliveTimeoutN = (int) keepAliveTimeout.get().toSeconds();
                options.setKeepAliveTimeout(keepAliveTimeoutN);
                options.setHttp2KeepAliveTimeout(keepAliveTimeoutN);
            }
            Optional<Duration> idleTimeout = config.idleTimeout();
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
                channel = new StorkGrpcChannel(client, config.host(), config.stork(), executor); // host = service-name
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

    private static GrpcClientConfiguration testConfig(GrpcServerConfiguration serverConfiguration) {
        if (serverConfiguration.ssl().certificate().isPresent() || serverConfiguration.ssl().keyStore().isPresent()) {
            LOGGER.warn("gRPC client created without configuration and the gRPC server is configured for SSL. " +
                    "Configuring SSL for such clients is not supported.");
        }

        return new GrpcClientConfiguration() {

            @Override
            public boolean useQuarkusGrpcClient() {
                return false;
            }

            @Override
            public boolean useVertxEventLoop() {
                return true;
            }

            @Override
            public ClientXds xds() {
                return null;
            }

            @Override
            public InProcess inProcess() {
                return null;
            }

            @Override
            public StorkConfig stork() {
                return null;
            }

            @Override
            public int port() {
                return serverConfiguration.testPort();
            }

            @Override
            public OptionalInt testPort() {
                return OptionalInt.empty();
            }

            @Override
            public String host() {
                return serverConfiguration.host();
            }

            @Override
            public SslClientConfig ssl() {
                return new SslClientConfig() {
                    @Override
                    public Optional<Path> certificate() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Path> key() {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Path> trustStore() {
                        return Optional.empty();
                    }
                };
            }

            @Override
            public Optional<String> tlsConfigurationName() {
                return Optional.empty();
            }

            @Override
            public TlsClientConfig tls() {
                return new TlsClientConfig() {
                    @Override
                    public boolean enabled() {
                        return false;
                    }

                    @Override
                    public boolean trustAll() {
                        return false;
                    }

                    @Override
                    public PemTrustCertConfiguration trustCertificatePem() {
                        return new PemTrustCertConfiguration() {
                            @Override
                            public Optional<List<String>> certs() {
                                return Optional.empty();
                            }
                        };
                    }

                    @Override
                    public JksConfiguration trustCertificateJks() {
                        return new JksConfiguration() {
                            @Override
                            public Optional<String> path() {
                                return Optional.empty();
                            }

                            @Override
                            public Optional<String> password() {
                                return Optional.empty();
                            }
                        };
                    }

                    @Override
                    public PfxConfiguration trustCertificateP12() {
                        return new PfxConfiguration() {
                            @Override
                            public Optional<String> path() {
                                return Optional.empty();
                            }

                            @Override
                            public Optional<String> password() {
                                return Optional.empty();
                            }
                        };
                    }

                    @Override
                    public PemKeyCertConfiguration keyCertificatePem() {
                        return new PemKeyCertConfiguration() {
                            @Override
                            public Optional<List<String>> keys() {
                                return Optional.empty();
                            }

                            @Override
                            public Optional<List<String>> certs() {
                                return Optional.empty();
                            }
                        };
                    }

                    @Override
                    public JksConfiguration keyCertificateJks() {
                        return new JksConfiguration() {
                            @Override
                            public Optional<String> path() {
                                return Optional.empty();
                            }

                            @Override
                            public Optional<String> password() {
                                return Optional.empty();
                            }
                        };
                    }

                    @Override
                    public PfxConfiguration keyCertificateP12() {
                        return new PfxConfiguration() {
                            @Override
                            public Optional<String> path() {
                                return Optional.empty();
                            }

                            @Override
                            public Optional<String> password() {
                                return Optional.empty();
                            }
                        };
                    }

                    @Override
                    public boolean verifyHostname() {
                        return false;
                    }
                };
            }

            @Override
            public String nameResolver() {
                return DNS;
            }

            @Override
            public Optional<Boolean> plainText() {
                return Optional.of(serverConfiguration.plainText());
            }

            @Override
            public Optional<Duration> keepAliveTime() {
                return Optional.empty();
            }

            @Override
            public OptionalInt flowControlWindow() {
                return OptionalInt.empty();
            }

            @Override
            public Optional<Duration> idleTimeout() {
                return Optional.empty();
            }

            @Override
            public Optional<Duration> keepAliveTimeout() {
                return Optional.empty();
            }

            @Override
            public boolean keepAliveWithoutCalls() {
                return false;
            }

            @Override
            public int maxHedgedAttempts() {
                return 5;
            }

            @Override
            public int maxRetryAttempts() {
                return 0;
            }

            @Override
            public OptionalInt maxTraceEvents() {
                return OptionalInt.empty();
            }

            @Override
            public OptionalInt maxInboundMessageSize() {
                return OptionalInt.empty();
            }

            @Override
            public OptionalInt maxInboundMetadataSize() {
                return OptionalInt.empty();
            }

            @Override
            public String negotiationType() {
                return "PLAINTEXT";
            }

            @Override
            public Optional<String> overrideAuthority() {
                return Optional.empty();
            }

            @Override
            public OptionalLong perRpcBufferLimit() {
                return OptionalLong.empty();
            }

            @Override
            public boolean retry() {
                return false;
            }

            @Override
            public OptionalLong retryBufferSize() {
                return OptionalLong.empty();
            }

            @Override
            public Optional<String> userAgent() {
                return Optional.empty();
            }

            @Override
            public String loadBalancingPolicy() {
                return "pick_first";
            }

            @Override
            public Optional<String> compression() {
                return Optional.empty();
            }

            @Override
            public Optional<Duration> deadline() {
                return Optional.empty();
            }
        };
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
