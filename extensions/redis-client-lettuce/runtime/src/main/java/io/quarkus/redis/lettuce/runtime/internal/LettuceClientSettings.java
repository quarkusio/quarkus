package io.quarkus.redis.lettuce.runtime.internal;

import static io.quarkus.redis.runtime.client.config.RedisConfig.getPropertyName;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxTrustOptions;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.ConfigValue;
import org.jboss.logging.Logger;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.CredentialsProvider;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.SslVerifyMode;
import io.lettuce.core.StaticCredentialsProvider;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.resource.Delay;
import io.lettuce.core.resource.NettyCustomizer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.quarkus.redis.runtime.client.config.NetConfig;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.smallrye.config.DefaultValuesConfigSource;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.TrustOptions;
import io.vertx.redis.client.RedisReplicas;
import io.vertx.redis.client.RedisRole;

/**
 * The Lettuce {@link RedisURI}s and {@link ClientOptions} derived from a {@code quarkus.redis[.<name>].*} client
 * configuration: the hosts, the credentials, the client name and the TLS settings, the protocol version, the
 * command queue bound and the TCP socket options, plus the cluster, Sentinel and replication settings
 * ({@link #readFrom}, {@link #topologyRefreshOptions} and {@link #sentinelUri}). The settings Lettuce only takes
 * from its {@link io.lettuce.core.resource.ClientResources}, the reconnect delay and the Netty channel options,
 * are exposed separately ({@link #reconnectDelay()} and {@link #nettyCustomizer()}) for
 * {@link LettuceClientResources#clientResources} to apply.
 * <p>
 * The credentials and TLS properties are interpreted like the Vert.x Redis client interprets them, except for the
 * differences documented below:
 * <ul>
 * <li>the credentials encoded in the URI take precedence over the {@code password} property, and are parsed the way
 * the Vert.x client parses them (see {@code UserInfo});</li>
 * <li>instead of re-implementing the {@code quarkus.redis.tls.*} and TLS registry handling, the settings are first
 * applied to a throwaway Vert.x {@link NetClientOptions} with the helpers the Vert.x backend uses, and the outcome
 * (trust and key material, trust-all, hostname verification, protocols and cipher suites) is then mapped onto the
 * Lettuce {@link SslOptions} and {@link SslVerifyMode}.</li>
 * </ul>
 * The deliberate differences from the Vert.x client are:
 * <ul>
 * <li>an empty password encoded in the URI ({@code redis://user:@host}) counts as absent, so the {@code password}
 * property applies, whereas the Vert.x client authenticates with the empty password;</li>
 * <li>the {@code user} and {@code password} query parameters are split and then URL-decoded, like the user info
 * (see {@code UserInfo});</li>
 * <li>the {@code LDAPS} hostname verification algorithm verifies the host name with the {@code HTTPS} rules, the
 * only ones Lettuce supports (see {@code verifyMode}).</li>
 * </ul>
 */
public final class LettuceClientSettings {

    private static final Logger LOGGER = Logger.getLogger(LettuceClientSettings.class);

    /** The default of {@code topology-cache-ttl}. */
    public static final Duration DEFAULT_TOPOLOGY_CACHE_TTL = Duration.ofSeconds(1);

    /** The Vert.x query parameter naming the connection ({@code redis://host?client=name}). */
    static final String CLIENT_QUERY_PARAMETER = "client";

    /**
     * Accepts every certificate chain, like the Vert.x trust-all option. It is deliberately a plain (not extended)
     * {@link X509TrustManager}: the JDK wraps it and keeps performing the endpoint identification when a hostname
     * verification algorithm is set, so {@code trust-all} disables the chain validation only, as with the Vert.x
     * client.
     */
    private static final X509TrustManager TRUST_ALL = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    private final List<RedisURI> redisUris;
    private final ClientOptions clientOptions;
    private final Optional<Delay> reconnectDelay;
    private final Optional<NettyCustomizer> nettyCustomizer;

    private LettuceClientSettings(List<RedisURI> redisUris, ClientOptions clientOptions, Optional<Delay> reconnectDelay,
            Optional<NettyCustomizer> nettyCustomizer) {
        this.redisUris = redisUris;
        this.clientOptions = clientOptions;
        this.reconnectDelay = reconnectDelay;
        this.nettyCustomizer = nettyCustomizer;
    }

    /**
     * Builds the Lettuce settings for a Redis client.
     *
     * @param name the Quarkus Redis client name
     * @param config the client configuration
     * @param hosts the configured Redis URIs, in configuration order; the standalone client connects to the first
     *        one, the cluster client discovers the topology from all of them
     * @param vertx the Vert.x instance, used to load the configured trust and key material
     * @param tlsRegistry the TLS registry providing the named TLS configurations
     * @return the settings, never {@code null}
     * @throws IllegalStateException if the referenced named TLS configuration does not exist or its material cannot
     *         be loaded
     */
    public static LettuceClientSettings create(String name, RedisClientConfig config, Collection<URI> hosts, Vertx vertx,
            TlsConfigurationRegistry tlsRegistry) {
        if (hosts.isEmpty()) {
            throw new IllegalArgumentException("At least one host is required for the Redis client " + name);
        }
        NetClientOptions net = new NetClientOptions();
        configureTls(name, config, tlsRegistry, net, hosts);

        ClientOptions.Builder options = ClientOptions.builder();
        // max-waiting-handlers bounds the commands queued on a connection. The Vert.x client sends every command
        // through a pooled connection, each with its own queue, whereas the ordinary commands share one connection
        // here, so its default (2048) would bound the whole client: the property is applied only when set, and
        // Lettuce does not bound the queue otherwise.
        if (isSet(getPropertyName(name, "max-waiting-handlers"))) {
            options.requestQueueSize(config.maxWaitingHandlers());
        }
        Optional<ProtocolVersion> protocolVersion = protocolVersion(config);
        if (protocolVersion.isPresent()) {
            options.protocolVersion(protocolVersion.get());
        }
        Optional<SocketOptions> socketOptions = socketOptions(config.tcp());
        if (socketOptions.isPresent()) {
            options.socketOptions(socketOptions.get());
        }
        if (net.isSsl()) {
            options.sslOptions(sslOptions(name, net, vertx));
        }

        // The TLS mode and the password property apply to every host alike, as with the Vert.x client; this is also
        // what Lettuce expects of the seed nodes of a cluster, as it connects to the nodes it discovers with the
        // settings of the first one.
        List<RedisURI> redisUris = new ArrayList<>(hosts.size());
        for (URI host : hosts) {
            RedisURI redisUri = RedisURI.create(host);
            // Lettuce parsed the credentials of the URI its own way; replace them with the Vert.x interpretation.
            redisUri.setCredentialsProvider(
                    credentials(UserInfo.parse(host), config.username().orElse(null), config.password().orElse(null)));
            redisUri.setSsl(net.isSsl());
            if (net.isSsl()) {
                redisUri.setVerifyPeer(verifyMode(net));
            }
            String clientName = clientName(name, config, host);
            if (clientName != null) {
                redisUri.setClientName(clientName);
            }
            redisUris.add(redisUri);
        }
        return new LettuceClientSettings(List.copyOf(redisUris), options.build(), reconnectDelay(name, config),
                nettyCustomizer(config.tcp()));
    }

    /**
     * The URI of the first configured host: the one the standalone client connects to.
     */
    public RedisURI redisUri() {
        return redisUris.getFirst();
    }

    /**
     * The URIs of all the configured hosts, in configuration order: the seed nodes of the cluster client.
     */
    public List<RedisURI> redisUris() {
        return redisUris;
    }

    /**
     * The URI of the master monitored by Redis Sentinel under {@code masterName}, the configured hosts being the
     * sentinels. The credentials, the TLS settings, the timeout and the client and library names of the hosts apply
     * to the data nodes (the master URI takes them from the first host) and to every sentinel alike (each sentinel
     * URI is a copy of its host URI: Lettuce authenticates a sentinel with the settings of its own URI, does not copy
     * them from the master URI, and runs the {@code SENTINEL} queries with the timeout of the sentinel URI), as with
     * the Vert.x client, which uses the same options for both. The database is the one of the first host; a sentinel
     * has no databases.
     */
    public RedisURI sentinelUri(String masterName) {
        return sentinelUri(redisUris, masterName);
    }

    static RedisURI sentinelUri(List<RedisURI> sentinels, String masterName) {
        RedisURI first = sentinels.getFirst();
        RedisURI.Builder master = RedisURI.builder()
                .withSentinelMasterId(masterName)
                .withSsl(first)
                .withAuthentication(first)
                .withTimeout(first.getTimeout())
                .withDatabase(first.getDatabase());
        if (first.getClientName() != null) {
            master.withClientName(first.getClientName());
        }
        if (first.getLibraryName() != null) {
            master.withLibraryName(first.getLibraryName());
        }
        if (first.getLibraryVersion() != null) {
            master.withLibraryVersion(first.getLibraryVersion());
        }
        for (RedisURI host : sentinels) {
            master.withSentinel(RedisURI.builder(host).withDatabase(0).build());
        }
        return master.build();
    }

    public ClientOptions clientOptions() {
        return clientOptions;
    }

    /**
     * The delay between the attempts to reconnect a lost connection, when {@code reconnect-interval} is set (see
     * {@link #reconnectDelay(RedisClientConfig)}); a setting of the client resources, not of the client.
     */
    public Optional<Delay> reconnectDelay() {
        return reconnectDelay;
    }

    /**
     * The Netty channel options of the connections, when any of the {@code tcp.*} socket properties they come from
     * is set (see {@link #nettyCustomizer(NetConfig)}); a setting of the client resources, not of the client.
     */
    public Optional<NettyCustomizer> nettyCustomizer() {
        return nettyCustomizer;
    }

    /**
     * Maps {@code protocol-negotiation} and {@code preferred-protocol-version} onto the protocol version Lettuce
     * requests in its handshake. With the negotiation on (the default) and no preferred version, or RESP3 preferred,
     * the version is left to Lettuce, which sends {@code HELLO 3} and falls back to RESP2 when the server does not
     * know {@code HELLO}, as the Vert.x client does with its preferred version. RESP2 preferred, or the negotiation
     * off (the Vert.x client then skips {@code HELLO}), selects RESP2: Lettuce does not send {@code HELLO} either and
     * authenticates with {@code AUTH}.
     *
     * @return the protocol version to request, empty to let Lettuce negotiate the newest one the server supports
     */
    static Optional<ProtocolVersion> protocolVersion(RedisClientConfig config) {
        // the Vert.x and the Lettuce enumerations share their simple name; the configuration holds the Vert.x one
        boolean resp2Preferred = config.preferredProtocolVersion().isPresent()
                && config.preferredProtocolVersion().get() == io.vertx.redis.client.ProtocolVersion.RESP2;
        if (!config.protocolNegotiation() || resp2Preferred) {
            return Optional.of(ProtocolVersion.RESP2);
        }
        return Optional.empty();
    }

    /**
     * The name a connection to {@code host} registers with the server ({@code CLIENT SETNAME}, or {@code SETNAME} of
     * {@code HELLO} with RESP3), as the Vert.x client sets it: the {@code client} query parameter of the URI when it
     * carries one, otherwise, only when {@code configure-client-name} is set, {@code client-name} or, without it, the
     * Quarkus client name. As with the Vert.x client, {@code client-name} alone is not applied (the recorder reports
     * it). Lettuce applied its own {@code clientName} query parameter when it parsed the URI, so that one works too.
     * A cluster client names the connections to the nodes it discovers after the first seed, and a Sentinel or
     * replication client those to the master and the replicas after the first host.
     *
     * @return the client name, {@code null} when none is configured
     */
    static String clientName(String name, RedisClientConfig config, URI host) {
        String fromUri = UserInfo.queryParameters(host).get(CLIENT_QUERY_PARAMETER);
        if (fromUri != null && !fromUri.isEmpty()) {
            if (config.configureClientName()) {
                LOGGER.warnf("Your host already has a client name. The client name %s will be disregarded.",
                        config.clientName().orElse(name));
            }
            return fromUri;
        }
        if (config.configureClientName()) {
            return config.clientName().orElse(name);
        }
        return null;
    }

    /**
     * Maps {@code tcp.connection-timeout}, {@code tcp.keep-alive} and {@code tcp.no-delay} onto the Lettuce
     * {@link SocketOptions}. Only the set properties are applied: the Lettuce defaults (ten seconds connect timeout,
     * keep-alive and no-delay on) hold otherwise, not the Vert.x ones. A connect timeout that is not positive (zero
     * is no timeout on the Vert.x client) is not applied either, Lettuce requiring a positive one; the recorder
     * reports it.
     *
     * @return the socket options, empty when none of the three properties is applied
     */
    static Optional<SocketOptions> socketOptions(NetConfig tcp) {
        Optional<Duration> connectTimeout = tcp.connectionTimeout().filter(LettuceClientSettings::isPositive);
        if (connectTimeout.isEmpty() && tcp.keepAlive().isEmpty() && tcp.noDelay().isEmpty()) {
            return Optional.empty();
        }
        SocketOptions.Builder socket = SocketOptions.builder();
        if (connectTimeout.isPresent()) {
            socket.connectTimeout(connectTimeout.get());
        }
        if (tcp.keepAlive().isPresent()) {
            socket.keepAlive(tcp.keepAlive().get());
        }
        if (tcp.noDelay().isPresent()) {
            socket.tcpNoDelay(tcp.noDelay().get());
        }
        return Optional.of(socket.build());
    }

    /**
     * Maps {@code reconnect-interval}, overridden by {@code tcp.reconnect-interval} as with the Vert.x client, onto
     * the delay between the attempts Lettuce makes to reconnect a lost connection. Lettuce reconnects until it
     * succeeds and does not retry the initial connection, so {@code reconnect-attempts} has no equivalent (the
     * recorder reports it). The mapped values cannot tell a property set to the default interval (one second) from
     * an unset one, so the configuration is asked which of the two properties is set (see {@link #isSet}); the delay
     * is constant only then, and by default Lettuce backs off exponentially, from 100 milliseconds up to 30 seconds,
     * instead of retrying every second. (The default of {@code quarkus.redis.*.reconnect-interval} is registered for
     * every client and also matches {@code quarkus.redis.tcp.reconnect-interval}, so the mapped {@code tcp} property
     * of the default client is always present, with the default: another reason not to trust the mapped values.)
     *
     * @return the constant delay, empty when neither property is set
     */
    static Optional<Delay> reconnectDelay(String name, RedisClientConfig config) {
        Optional<Duration> tcpInterval = config.tcp().reconnectInterval();
        if (tcpInterval.isPresent() && isSet(getPropertyName(name, "tcp.reconnect-interval"))) {
            return Optional.of(Delay.constant(tcpInterval.get()));
        }
        if (isSet(getPropertyName(name, "reconnect-interval"))) {
            return Optional.of(Delay.constant(config.reconnectInterval()));
        }
        return Optional.empty();
    }

    static boolean isSet(String property) {
        ConfigValue value = ConfigProvider.getConfig().getConfigValue(property);
        return value.getValue() != null && !value.getValue().isEmpty()
                && value.getSourceOrdinal() > DefaultValuesConfigSource.ORDINAL;
    }

    static boolean isPositive(Duration duration) {
        return duration != null && !duration.isNegative() && !duration.isZero();
    }

    /**
     * Maps the {@code tcp.*} socket properties Lettuce has no option of its own for onto the Netty channel options
     * of the connections, set on the bootstrap the client connects with: {@code receive-buffer-size},
     * {@code send-buffer-size}, {@code so-linger} (in seconds, the unit of the socket option), {@code traffic-class},
     * {@code reuse-address} and {@code local-address}.
     *
     * @return the customizer, empty when none of the properties is set
     */
    static Optional<NettyCustomizer> nettyCustomizer(NetConfig tcp) {
        Map<ChannelOption<?>, Object> options = new LinkedHashMap<>();
        if (tcp.receiveBufferSize().isPresent()) {
            options.put(ChannelOption.SO_RCVBUF, tcp.receiveBufferSize().getAsInt());
        }
        if (tcp.sendBufferSize().isPresent()) {
            options.put(ChannelOption.SO_SNDBUF, tcp.sendBufferSize().getAsInt());
        }
        if (tcp.soLinger().isPresent()) {
            options.put(ChannelOption.SO_LINGER, (int) tcp.soLinger().get().toSeconds());
        }
        if (tcp.trafficClass().isPresent()) {
            options.put(ChannelOption.IP_TOS, tcp.trafficClass().getAsInt());
        }
        if (tcp.reuseAddress().isPresent()) {
            options.put(ChannelOption.SO_REUSEADDR, tcp.reuseAddress().get());
        }
        SocketAddress localAddress = null;
        if (tcp.localAddress().isPresent()) {
            localAddress = new InetSocketAddress(tcp.localAddress().get(), 0);
        }
        if (options.isEmpty() && localAddress == null) {
            return Optional.empty();
        }
        return Optional.of(new ChannelCustomizer(options, localAddress));
    }

    /**
     * Sets the channel options and the local address derived from the {@code tcp.*} properties on the bootstrap
     * Lettuce opens its connections with (see {@link #nettyCustomizer(NetConfig)}).
     */
    static final class ChannelCustomizer implements NettyCustomizer {

        private final Map<ChannelOption<?>, Object> options;
        private final SocketAddress localAddress;

        ChannelCustomizer(Map<ChannelOption<?>, Object> options, SocketAddress localAddress) {
            this.options = Map.copyOf(options);
            this.localAddress = localAddress;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void afterBootstrapInitialized(Bootstrap bootstrap) {
            for (Map.Entry<ChannelOption<?>, Object> option : options.entrySet()) {
                bootstrap.option((ChannelOption<Object>) option.getKey(), option.getValue());
            }
            if (localAddress != null) {
                bootstrap.localAddress(localAddress);
            }
        }

    }

    /**
     * Maps {@code quarkus.redis[.<name>].replicas} onto the nodes a cluster connection sends its read-only commands
     * to: {@code NEVER} (the default) reads from the upstream nodes only, {@code SHARE} from any node, {@code ALWAYS}
     * from the replicas, falling back to the upstream node of a shard that has no usable replica, as the Vert.x
     * client does ({@link ReadFrom#REPLICA} would fail such reads instead). As with the Vert.x client, writes always
     * go to the upstream nodes.
     */
    public static ReadFrom readFrom(Optional<RedisReplicas> replicas) {
        return replicas.map(redisReplicas -> switch (redisReplicas) {
            case NEVER -> ReadFrom.UPSTREAM;
            case SHARE -> ReadFrom.ANY;
            case ALWAYS -> ReadFrom.REPLICA_PREFERRED;
        }).orElse(ReadFrom.UPSTREAM);
    }

    /**
     * The nodes a Sentinel-managed connection reads from. {@code role=replica} reads from the replicas (falling back
     * to the master when none is usable); the writes keep going to the master, whereas the Vert.x client sends every
     * command of such a client to a replica and lets the writes fail there. Otherwise {@code replicas} decides, as
     * for a cluster ({@link #readFrom(Optional)}); the Vert.x client ignores {@code replicas} in Sentinel mode.
     * {@code role=sentinel} is rejected before this is called: a sentinel has no data commands.
     */
    public static ReadFrom readFrom(Optional<RedisRole> role, Optional<RedisReplicas> replicas) {
        if (role.isPresent() && role.get() == RedisRole.REPLICA) {
            return ReadFrom.REPLICA_PREFERRED;
        }
        return readFrom(replicas);
    }

    /**
     * How a cluster client keeps its view of the cluster topology up to date: it is refreshed periodically, every
     * {@code quarkus.redis[.<name>].topology-cache-ttl} (see {@link #topologyCacheTtl}), and adaptively on
     * {@code MOVED} and {@code ASK} redirects, on uncovered slots and unknown nodes, and when a node keeps failing to
     * reconnect. A non-positive TTL, which disables the topology cache of the Vert.x client, disables the periodic
     * refresh and leaves the adaptive one.
     */
    public static ClusterTopologyRefreshOptions topologyRefreshOptions(Duration topologyCacheTtl) {
        ClusterTopologyRefreshOptions.Builder refresh = ClusterTopologyRefreshOptions.builder()
                .enableAllAdaptiveRefreshTriggers();
        if (!topologyCacheTtl.isZero() && !topologyCacheTtl.isNegative()) {
            refresh.enablePeriodicRefresh(topologyCacheTtl);
        }
        return refresh.build();
    }

    /**
     * The TTL of the topology cache, {@code topology-cache-ttl}, which always has its default.
     */
    public static Duration topologyCacheTtl(RedisClientConfig config) {
        return config.topologyCacheTtl().orElse(DEFAULT_TOPOLOGY_CACHE_TTL);
    }

    /**
     * Combines the credentials of the URI with the {@code username} and {@code password} properties. Like with the
     * Vert.x Redis client, the properties are the defaults: a username or a password encoded in the URI takes
     * precedence over the corresponding property. A username without password does not authenticate.
     *
     * @param userInfo the credentials encoded in the URI
     * @param usernameProperty the {@code username} property, {@code null} when not set
     * @param passwordProperty the {@code password} property, {@code null} when not set
     */
    static CredentialsProvider credentials(UserInfo userInfo, String usernameProperty, String passwordProperty) {
        String username = userInfo.username() != null ? userInfo.username() : usernameProperty;
        String password = userInfo.password() != null ? userInfo.password() : passwordProperty;
        return new StaticCredentialsProvider(username, password == null ? null : password.toCharArray());
    }

    /**
     * The credentials encoded in a Redis URI, interpreted as the Vert.x Redis client does: {@code user:password@},
     * {@code :password@} or {@code user@} (a lone user info is the username, not the password as in Lettuce), each
     * part URL-decoded, with the {@code user} and {@code password} query parameters as fallbacks. Empty parts count
     * as absent.
     * <p>
     * The query parameters deliberately differ from the Vert.x client in one respect: they are split on the raw
     * query and then URL-decoded, the same way as the user info. The Vert.x client splits the already decoded query
     * and does not decode the values, so a percent-encoded {@code &} or {@code =} in a value truncates the value
     * there, and a {@code +} stays a plus sign there while it is a space here.
     *
     * @param username the username, {@code null} when the URI does not carry one
     * @param password the password, {@code null} when the URI does not carry one
     */
    record UserInfo(String username, String password) {

        static final UserInfo NONE = new UserInfo(null, null);

        static UserInfo parse(URI uri) {
            String username = null;
            String password = null;
            String userInfo = uri.getRawUserInfo();
            if (userInfo != null && !userInfo.isEmpty()) {
                int colon = userInfo.indexOf(':');
                if (colon < 0) {
                    username = decode(userInfo);
                } else {
                    if (colon > 0) {
                        username = decode(userInfo.substring(0, colon));
                    }
                    password = decode(userInfo.substring(colon + 1));
                }
            }
            Map<String, String> query = queryParameters(uri);
            if (username == null) {
                username = query.get("user");
            }
            if (password == null) {
                password = query.get("password");
            }
            return new UserInfo(emptyToNull(username), emptyToNull(password));
        }

        private static Map<String, String> queryParameters(URI uri) {
            String query = uri.getRawQuery();
            if (query == null) {
                return Map.of();
            }
            Map<String, String> parameters = new HashMap<>();
            for (String parameter : query.split("&")) {
                int equals = parameter.indexOf('=');
                if (equals > 0) {
                    parameters.put(decode(parameter.substring(0, equals)), decode(parameter.substring(equals + 1)));
                }
            }
            return parameters;
        }

        private static String decode(String value) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }

        private static String emptyToNull(String value) {
            return value == null || value.isEmpty() ? null : value;
        }
    }

    /**
     * Mirrors {@code VertxRedisClientFactory.configureTLS} (and the TLS-relevant part of its TCP configuration), so
     * that both backends read the {@code tls.*}, {@code tls-configuration-name} and
     * {@code tcp.secure-transport-protocols} properties the same way: one {@code rediss://} host enables TLS for all.
     */
    private static void configureTls(String name, RedisClientConfig config, TlsConfigurationRegistry tlsRegistry,
            NetClientOptions net, Collection<URI> hosts) {
        TlsConfiguration configuration = null;
        boolean defaultTrustAll = false;

        boolean tlsFromHosts = false;
        for (URI host : hosts) {
            if ("rediss".equals(host.getScheme())) {
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

        config.tcp().secureTransportProtocols().ifPresent(net::setEnabledSecureTransportProtocols);

        // Apply the configuration
        if (configuration != null) {
            TlsConfigUtils.configure(net, configuration);
            net.setSsl(tlsFromHosts);
        } else {
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

    /**
     * Maps the Vert.x hostname verification algorithm onto the Lettuce {@link SslVerifyMode}: an empty algorithm, or
     * {@code NONE} as the TLS registry passes it on verbatim from {@code hostname-verification-algorithm}, verifies
     * the certificate chain only; any other algorithm additionally verifies the host name (Lettuce only supports the
     * HTTPS algorithm). Trust-all is not a verify mode but a trust manager (see {@link #TRUST_ALL}), so that, like
     * with the Vert.x client, it does not switch the hostname verification off.
     */
    static SslVerifyMode verifyMode(NetClientOptions net) {
        String algorithm = net.getHostnameVerificationAlgorithm();
        if (algorithm == null || algorithm.isEmpty() || "NONE".equalsIgnoreCase(algorithm)) {
            return SslVerifyMode.CA;
        }
        return SslVerifyMode.FULL;
    }

    private static SslOptions sslOptions(String name, NetClientOptions net, Vertx vertx) {
        SslOptions.Builder ssl = SslOptions.builder();
        try {
            if (net.isTrustAll()) {
                ssl.trustManager(TrustOptions.wrap(TRUST_ALL).getTrustManagerFactory(vertx));
            } else if (net.getTrustOptions() != null) {
                TrustManagerFactory trustManagerFactory = net.getTrustOptions().getTrustManagerFactory(vertx);
                if (trustManagerFactory != null) {
                    ssl.trustManager(trustManagerFactory);
                }
            }
            if (net.getKeyCertOptions() != null) {
                KeyManagerFactory keyManagerFactory = net.getKeyCertOptions().getKeyManagerFactory(vertx);
                if (keyManagerFactory != null) {
                    ssl.keyManager(keyManagerFactory);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load the TLS trust or key material of the Redis client " + name, e);
        }
        if (net.getEnabledSecureTransportProtocols() != null && !net.getEnabledSecureTransportProtocols().isEmpty()) {
            ssl.protocols(net.getEnabledSecureTransportProtocols().toArray(String[]::new));
        }
        if (net.getEnabledCipherSuites() != null && !net.getEnabledCipherSuites().isEmpty()) {
            ssl.cipherSuites(net.getEnabledCipherSuites().toArray(String[]::new));
        }
        ssl.handshakeTimeout(Duration.of(net.getSslHandshakeTimeout(), net.getSslHandshakeTimeoutUnit().toChronoUnit()));
        return ssl.build();
    }

}
