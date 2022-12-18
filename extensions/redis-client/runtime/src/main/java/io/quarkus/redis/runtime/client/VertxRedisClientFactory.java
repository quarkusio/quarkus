package io.quarkus.redis.runtime.client;

import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxTrustOptions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.redis.client.RedisHostsProvider;
import io.quarkus.redis.client.RedisOptionsCustomizer;
import io.quarkus.redis.runtime.client.config.NetConfig;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.redis.runtime.client.config.TlsConfig;
import io.quarkus.runtime.configuration.ConfigurationException;
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

    private VertxRedisClientFactory() {
        // Avoid direct instantiation.
    }

    public static Redis create(String name, Vertx vertx, RedisClientConfig config) {
        RedisOptions options = new RedisOptions();

        List<URI> hosts = new ArrayList<>();
        if (config.hosts.isPresent()) {
            hosts.addAll(config.hosts.get());
            for (URI uri : config.hosts.get()) {
                options.addConnectionString(uri.toString().trim());
            }
        } else if (config.hostsProviderName.isPresent()) {
            RedisHostsProvider hostsProvider = findProvider(config.hostsProviderName.get());
            hosts.addAll(hostsProvider.getHosts());
            for (URI uri : hostsProvider.getHosts()) {
                options.addConnectionString(uri.toString());
            }
        } else {
            throw new ConfigurationException("Redis host not configured - you must either configure 'quarkus.redis.hosts` or" +
                    " 'quarkus.redis.host-provider-name' and have a bean providing the hosts programmatically.");
        }

        if (RedisClientType.STANDALONE == config.clientType) {
            if (hosts.size() > 1) {
                throw new ConfigurationException("Multiple Redis hosts supplied for non-clustered configuration");
            }
        }

        config.masterName.ifPresent(options::setMasterName);
        options.setMaxNestedArrays(config.maxNestedArrays);
        options.setMaxPoolSize(config.maxPoolSize);
        options.setMaxPoolWaiting(config.maxPoolWaiting);
        options.setMaxWaitingHandlers(config.maxWaitingHandlers);

        options.setProtocolNegotiation(config.protocolNegotiation);
        options.setPassword(config.password.orElse(null));
        config.poolCleanerInterval.ifPresent(d -> options.setPoolCleanerInterval((int) d.toMillis()));
        options.setPoolRecycleTimeout((int) config.poolRecycleTimeout.toMillis());

        options.setPoolName(name);

        config.role.ifPresent(options::setRole);
        options.setType(config.clientType);
        config.replicas.ifPresent(options::setUseReplicas);

        options.setNetClientOptions(toNetClientOptions(config));

        customize(name, options);

        return Redis.createClient(vertx, options);
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
        NetConfig tcp = config.tcp;
        TlsConfig tls = config.tls;
        NetClientOptions net = new NetClientOptions();

        tcp.alpn.ifPresent(net::setUseAlpn);
        tcp.applicationLayerProtocols.ifPresent(net::setApplicationLayerProtocols);
        tcp.connectionTimeout.ifPresent(d -> net.setConnectTimeout((int) d.toMillis()));
        tls.hostnameVerificationAlgorithm.ifPresent(net::setHostnameVerificationAlgorithm);
        tcp.idleTimeout.ifPresent(d -> net.setIdleTimeout((int) d.toSeconds()));

        tcp.keepAlive.ifPresent(b -> net.setTcpKeepAlive(true));
        tcp.noDelay.ifPresent(b -> net.setTcpNoDelay(true));

        net.setSsl(tls.enabled).setTrustAll(tls.trustAll);

        configurePemTrustOptions(net, tls.trustCertificatePem);
        configureJksTrustOptions(net, tls.trustCertificateJks);
        configurePfxTrustOptions(net, tls.trustCertificatePfx);

        configurePemKeyCertOptions(net, tls.keyCertificatePem);
        configureJksKeyCertOptions(net, tls.keyCertificateJks);
        configurePfxKeyCertOptions(net, tls.keyCertificatePfx);

        net.setReconnectAttempts(config.reconnectAttempts);
        net.setReconnectInterval(config.reconnectInterval.toMillis());

        tcp.localAddress.ifPresent(net::setLocalAddress);
        tcp.nonProxyHosts.ifPresent(net::setNonProxyHosts);
        tcp.proxyOptions.ifPresent(s -> {
            ProxyOptions po = new ProxyOptions();
            po.setHost(s.host);
            po.setType(s.type);
            po.setPort(s.port);
            s.username.ifPresent(po::setUsername);
            s.password.ifPresent(po::setPassword);
            net.setProxyOptions(po);
        });
        tcp.readIdleTimeout.ifPresent(d -> net.setReadIdleTimeout((int) d.toSeconds()));
        tcp.reconnectAttempts.ifPresent(net::setReconnectAttempts);
        tcp.reconnectInterval.ifPresent(v -> net.setReconnectInterval(v.toMillis()));
        tcp.reuseAddress.ifPresent(net::setReuseAddress);
        tcp.reusePort.ifPresent(net::setReusePort);
        tcp.receiveBufferSize.ifPresent(net::setReceiveBufferSize);
        tcp.sendBufferSize.ifPresent(net::setSendBufferSize);
        tcp.soLinger.ifPresent(d -> net.setSoLinger((int) d.toMillis()));
        tcp.secureTransportProtocols.ifPresent(net::setEnabledSecureTransportProtocols);
        tcp.trafficClass.ifPresent(net::setTrafficClass);
        tcp.noDelay.ifPresent(net::setTcpNoDelay);
        tcp.cork.ifPresent(net::setTcpCork);
        tcp.keepAlive.ifPresent(net::setTcpKeepAlive);
        tcp.fastOpen.ifPresent(net::setTcpFastOpen);
        tcp.quickAck.ifPresent(net::setTcpQuickAck);
        tcp.writeIdleTimeout.ifPresent(d -> net.setWriteIdleTimeout((int) d.toSeconds()));

        tls.hostnameVerificationAlgorithm.ifPresent(net::setHostnameVerificationAlgorithm);

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

}
