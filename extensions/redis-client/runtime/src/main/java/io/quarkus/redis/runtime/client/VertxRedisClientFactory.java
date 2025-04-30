package io.quarkus.redis.runtime.client;

import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxTrustOptions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.redis.client.RedisHostsProvider;
import io.quarkus.redis.client.RedisOptionsCustomizer;
import io.quarkus.redis.runtime.client.config.NetConfig;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;

/**
 * Creates Vert.x Redis client for a given {@link RedisClientConfig}.
 */
public class VertxRedisClientFactory {

    public static final String DEFAULT_CLIENT = "<default>";
    public static String NON_RESERVED_URI_PATTERN = "[^a-zA-Z0-9\\-_.~]";

    private static final Logger LOGGER = Logger.getLogger(VertxRedisClientFactory.class);

    private VertxRedisClientFactory() {
        // Avoid direct instantiation.
    }

    public static Redis create(String name, Vertx vertx, RedisClientConfig config, TlsConfigurationRegistry tlsRegistry) {
        RedisOptions options = new RedisOptions();

        Consumer<Set<URI>> configureOptions = new Consumer<Set<URI>>() {
            @Override
            public void accept(Set<URI> uris) {
                for (URI uri : uris) {
                    if (config.configureClientName()) {
                        String client = config.clientName().orElse(name);
                        String newURI = applyClientQueryParam(client, uri);
                        options.addConnectionString(newURI);
                    } else {
                        options.addConnectionString(uri.toString().trim());
                    }
                }
            }
        };

        List<URI> hosts = new ArrayList<>();
        if (config.hosts().isPresent()) {
            hosts.addAll(config.hosts().get());
            configureOptions.accept(config.hosts().get());
        } else if (config.hostsProviderName().isPresent()) {
            RedisHostsProvider hostsProvider = findProvider(config.hostsProviderName().get());
            Set<URI> computedHosts = hostsProvider.getHosts();
            hosts.addAll(computedHosts);
            configureOptions.accept(computedHosts);
        } else {
            throw new ConfigurationException("Redis host not configured - you must either configure 'quarkus.redis.hosts` or" +
                    " 'quarkus.redis.host-provider-name' and have a bean providing the hosts programmatically.");
        }

        if (RedisClientType.STANDALONE == config.clientType()) {
            if (hosts.size() > 1) {
                throw new ConfigurationException("Multiple Redis hosts supplied for non-clustered configuration");
            }
        }

        config.masterName().ifPresent(options::setMasterName);
        options.setMaxNestedArrays(config.maxNestedArrays());
        options.setMaxPoolSize(config.maxPoolSize());
        options.setMaxPoolWaiting(config.maxPoolWaiting());
        options.setMaxWaitingHandlers(config.maxWaitingHandlers());

        options.setProtocolNegotiation(config.protocolNegotiation());
        config.preferredProtocolVersion().ifPresent(options::setPreferredProtocolVersion);
        options.setPassword(config.password().orElse(null));
        config.poolCleanerInterval().ifPresent(d -> options.setPoolCleanerInterval((int) d.toMillis()));
        config.poolRecycleTimeout().ifPresent(d -> options.setPoolRecycleTimeout((int) d.toMillis()));
        options.setHashSlotCacheTTL(config.hashSlotCacheTtl().toMillis());

        config.role().ifPresent(options::setRole);
        options.setType(config.clientType());
        config.replicas().ifPresent(options::setUseReplicas);
        options.setAutoFailover(config.autoFailover());
        config.topology().ifPresent(options::setTopology);

        options.setNetClientOptions(toNetClientOptions(config));
        configureTLS(name, config, tlsRegistry, options.getNetClientOptions(), hosts);

        options.setPoolName(name);
        // Use the convention defined by Quarkus Micrometer Vert.x metrics to create metrics prefixed with redis.
        // and the client_name as tag.
        // See io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractPrefix and
        // io.quarkus.micrometer.runtime.binder.vertx.VertxMeterBinderAdapter.extractClientName
        options.getNetClientOptions().setMetricsName("redis|" + name);

        customize(name, options);

        return Redis.createClient(vertx, options);
    }

    public static String applyClientQueryParam(String client, URI uri) {

        if (client.matches(".*" + NON_RESERVED_URI_PATTERN + ".*")) {
            LOGGER.warn("The client query parameter contains reserved URI characters. " +
                    "This may result in an incorrect client name after URI encoding.");
        }

        String query = uri.getQuery();

        boolean hasClient = hasRedisClientParameter(query);

        if (hasClient) {
            LOGGER.warnf("Your host already has a client name. The client name %s will be disregarded.", client);
            return uri.toString().trim();
        }

        query = query == null ? "client=" + client
                : uri.getQuery() + "&client=" + client;

        try {
            return new URI(
                    uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment()).toString().trim();
        } catch (URISyntaxException e) {
            LOGGER.warnf("Was not possible to generate a new Redis URL with client query parameter, " +
                    "the value is: %s", client);
            return uri.toString().trim();
        }
    }

    private static boolean hasRedisClientParameter(String query) {
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2 && keyValue[0].equals("client")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void customize(String name, RedisOptions options) {
        if (Arc.container() != null) {
            List<InstanceHandle<RedisOptionsCustomizer>> customizers = Arc.container().listAll(RedisOptionsCustomizer.class);
            for (InstanceHandle<RedisOptionsCustomizer> customizer : customizers) {
                customizer.get().customize(name, options);
            }
        }
    }

    private static NetClientOptions toNetClientOptions(RedisClientConfig config) {
        NetConfig tcp = config.tcp();
        NetClientOptions net = new NetClientOptions();

        tcp.applicationLayerProtocols().ifPresent(net::setApplicationLayerProtocols);
        tcp.connectionTimeout().ifPresent(d -> net.setConnectTimeout((int) d.toMillis()));
        tcp.idleTimeout().ifPresent(d -> net.setIdleTimeout((int) d.toSeconds()));
        tcp.keepAlive().ifPresent(b -> net.setTcpKeepAlive(true));
        tcp.noDelay().ifPresent(b -> net.setTcpNoDelay(true));

        net.setReconnectAttempts(config.reconnectAttempts());
        net.setReconnectInterval(config.reconnectInterval().toMillis());

        tcp.localAddress().ifPresent(net::setLocalAddress);
        tcp.nonProxyHosts().ifPresent(net::setNonProxyHosts);
        if (tcp.proxyOptions().host().isPresent()) {
            ProxyOptions po = new ProxyOptions();
            po.setHost(tcp.proxyOptions().host().get());
            po.setType(tcp.proxyOptions().type());
            po.setPort(tcp.proxyOptions().port());
            tcp.proxyOptions().username().ifPresent(po::setUsername);
            tcp.proxyOptions().password().ifPresent(po::setPassword);
            net.setProxyOptions(po);
        }
        tcp.readIdleTimeout().ifPresent(d -> net.setReadIdleTimeout((int) d.toSeconds()));
        tcp.reconnectAttempts().ifPresent(net::setReconnectAttempts);
        tcp.reconnectInterval().ifPresent(v -> net.setReconnectInterval(v.toMillis()));
        tcp.reuseAddress().ifPresent(net::setReuseAddress);
        tcp.reusePort().ifPresent(net::setReusePort);
        tcp.receiveBufferSize().ifPresent(net::setReceiveBufferSize);
        tcp.sendBufferSize().ifPresent(net::setSendBufferSize);
        tcp.soLinger().ifPresent(d -> net.setSoLinger((int) d.toMillis()));
        tcp.secureTransportProtocols().ifPresent(net::setEnabledSecureTransportProtocols);
        tcp.trafficClass().ifPresent(net::setTrafficClass);
        tcp.noDelay().ifPresent(net::setTcpNoDelay);
        tcp.cork().ifPresent(net::setTcpCork);
        tcp.keepAlive().ifPresent(net::setTcpKeepAlive);
        tcp.fastOpen().ifPresent(net::setTcpFastOpen);
        tcp.quickAck().ifPresent(net::setTcpQuickAck);
        tcp.writeIdleTimeout().ifPresent(d -> net.setWriteIdleTimeout((int) d.toSeconds()));

        return net;
    }

    public static RedisHostsProvider findProvider(String name) {
        ArcContainer container = Arc.container();
        InjectableInstance<RedisHostsProvider> providers;
        if (name != null) {
            providers = container.select(RedisHostsProvider.class, Identifier.Literal.of(name));
            if (providers.isUnsatisfied()) {
                throw new ConfigurationException("Unable to find redis host provider identified with " + name);
            }
        } else {
            providers = container.select(RedisHostsProvider.class);
            if (providers.isUnsatisfied()) {
                throw new ConfigurationException("Unable to find redis host provider");
            }
        }

        return providers.get();
    }

    private static void configureTLS(String name, RedisClientConfig config, TlsConfigurationRegistry tlsRegistry,
            NetClientOptions net, List<URI> hosts) {
        TlsConfiguration configuration = null;
        boolean defaultTrustAll = false;

        boolean tlsFromHosts = false;
        for (URI uri : hosts) {
            if ("rediss".equals(uri.getScheme())) {
                tlsFromHosts = true;
                break;
            }
        }

        // Check if we have a named TLS configuration or a default configuration:
        if (config.tlsConfigurationName().isPresent()) {
            Optional<TlsConfiguration> maybeConfiguration = tlsRegistry.get(config.tlsConfigurationName().get());
            if (maybeConfiguration.isEmpty()) {
                throw new IllegalStateException("Unable to find the TLS configuration "
                        + config.tlsConfigurationName().get() + " for the Redis client " + name + ".");
            }
            configuration = maybeConfiguration.get();
        } else if (tlsRegistry.getDefault().isPresent() && (tlsRegistry.getDefault().get().isTrustAll())) {
            defaultTrustAll = tlsRegistry.getDefault().get().isTrustAll();
            if (defaultTrustAll) {
                LOGGER.warn("The default TLS configuration is set to trust all certificates. This is a security risk."
                        + "Please use a named TLS configuration for the Redis client " + name + " to avoid this warning.");
            }
        }

        if (configuration != null && !tlsFromHosts) {
            LOGGER.warnf("The Redis client %s is configured with a named TLS configuration but the hosts are not " +
                    "using the `rediss://` scheme - Disabling TLS", name);
        }

        // Apply the configuration
        if (configuration != null) {
            // This part is often the same (or close) for every Vert.x client:
            TlsConfigUtils.configure(net, configuration);
            net.setSsl(tlsFromHosts);
        } else {
            config.tcp().alpn().ifPresent(net::setUseAlpn);

            String verificationAlgorithm = config.tls().hostnameVerificationAlgorithm();
            if ("NONE".equalsIgnoreCase(verificationAlgorithm)) {
                net.setHostnameVerificationAlgorithm("");
            } else {
                net.setHostnameVerificationAlgorithm(verificationAlgorithm);
            }
            net.setSsl(config.tls().enabled() || tlsFromHosts);
            net.setTrustAll(config.tls().trustAll() || defaultTrustAll);

            configurePemTrustOptions(net, config.tls().trustCertificatePem());
            configureJksTrustOptions(net, config.tls().trustCertificateJks());
            configurePfxTrustOptions(net, config.tls().trustCertificatePfx());

            configurePemKeyCertOptions(net, config.tls().keyCertificatePem());
            configureJksKeyCertOptions(net, config.tls().keyCertificateJks());
            configurePfxKeyCertOptions(net, config.tls().keyCertificatePfx());
        }
    }

}
