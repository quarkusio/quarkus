package io.quarkus.redis.lettuce.runtime.internal;

import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configureJksTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePemTrustOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxKeyCertOptions;
import static io.quarkus.vertx.core.runtime.SSLConfigHelper.configurePfxTrustOptions;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.jboss.logging.Logger;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.CredentialsProvider;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslOptions;
import io.lettuce.core.SslVerifyMode;
import io.lettuce.core.StaticCredentialsProvider;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.TrustOptions;
import io.vertx.redis.client.RedisReplicas;
import io.vertx.redis.client.RedisRole;

/**
 * The Lettuce {@link RedisURI}s and {@link ClientOptions} derived from a {@code quarkus.redis[.<name>].*} client
 * configuration: the hosts, the credentials and the TLS settings, plus the cluster, Sentinel and replication
 * settings ({@link #readFrom}, {@link #topologyRefreshOptions} and {@link #sentinelUri}).
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

    /** The default of {@code topology-cache-ttl} and of its deprecated alias {@code hash-slot-cache-ttl}. */
    public static final Duration DEFAULT_TOPOLOGY_CACHE_TTL = Duration.ofSeconds(1);

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

    private LettuceClientSettings(List<RedisURI> redisUris, ClientOptions clientOptions) {
        this.redisUris = redisUris;
        this.clientOptions = clientOptions;
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
            redisUri.setCredentialsProvider(credentials(UserInfo.parse(host), config.password().orElse(null)));
            redisUri.setSsl(net.isSsl());
            if (net.isSsl()) {
                redisUri.setVerifyPeer(verifyMode(net));
            }
            redisUris.add(redisUri);
        }
        return new LettuceClientSettings(List.copyOf(redisUris), options.build());
    }

    /**
     * The URI of the first configured host: the one the standalone client connects to.
     */
    public RedisURI redisUri() {
        return redisUris.get(0);
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
        RedisURI first = sentinels.get(0);
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
     * Maps {@code quarkus.redis[.<name>].replicas} onto the nodes a cluster connection sends its read-only commands
     * to: {@code NEVER} (the default) reads from the upstream nodes only, {@code SHARE} from any node, {@code ALWAYS}
     * from the replicas, falling back to the upstream node of a shard that has no usable replica, as the Vert.x
     * client does ({@link ReadFrom#REPLICA} would fail such reads instead). As with the Vert.x client, writes always
     * go to the upstream nodes.
     */
    public static ReadFrom readFrom(Optional<RedisReplicas> replicas) {
        return switch (replicas.orElse(RedisReplicas.NEVER)) {
            case NEVER -> ReadFrom.UPSTREAM;
            case SHARE -> ReadFrom.ANY;
            case ALWAYS -> ReadFrom.REPLICA_PREFERRED;
        };
    }

    /**
     * The nodes a Sentinel-managed connection reads from. {@code role=replica} reads from the replicas (falling back
     * to the master when none is usable); the writes keep going to the master, whereas the Vert.x client sends every
     * command of such a client to a replica and lets the writes fail there. Otherwise {@code replicas} decides, as
     * for a cluster ({@link #readFrom(Optional)}); the Vert.x client ignores {@code replicas} in Sentinel mode.
     * {@code role=sentinel} is rejected before this is called: a sentinel has no data commands.
     */
    public static ReadFrom readFrom(Optional<RedisRole> role, Optional<RedisReplicas> replicas) {
        if (role.orElse(RedisRole.MASTER) == RedisRole.REPLICA) {
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
     * The TTL of the topology cache, {@code topology-cache-ttl}. The property always has its default, so its
     * deprecated alias {@code hash-slot-cache-ttl} is not applied, by this backend or by the Vert.x one (whose
     * factory falls back on the alias only when {@code topology-cache-ttl} is absent, which it never is).
     */
    public static Duration topologyCacheTtl(RedisClientConfig config) {
        return config.topologyCacheTtl().orElse(DEFAULT_TOPOLOGY_CACHE_TTL);
    }

    /**
     * Combines the credentials of the URI with the {@code password} property. Like with the Vert.x Redis client, the
     * property is the default password: a password encoded in the URI takes precedence. The user name, if any,
     * always comes from the URI; a user name without password does not authenticate.
     *
     * @param userInfo the credentials encoded in the URI
     * @param passwordProperty the {@code password} property, {@code null} when not set
     */
    static CredentialsProvider credentials(UserInfo userInfo, String passwordProperty) {
        String password = userInfo.password() != null ? userInfo.password() : passwordProperty;
        return new StaticCredentialsProvider(userInfo.username(), password == null ? null : password.toCharArray());
    }

    /**
     * The credentials encoded in a Redis URI, interpreted as the Vert.x Redis client does: {@code user:password@},
     * {@code :password@} or {@code user@} (a lone user info is the user name, not the password as in Lettuce), each
     * part URL-decoded, with the {@code user} and {@code password} query parameters as fallbacks. Empty parts count
     * as absent.
     * <p>
     * The query parameters deliberately differ from the Vert.x client in one respect: they are split on the raw
     * query and then URL-decoded, the same way as the user info. The Vert.x client splits the already decoded query
     * and does not decode the values, so a percent-encoded {@code &} or {@code =} in a value truncates the value
     * there, and a {@code +} stays a plus sign there while it is a space here.
     *
     * @param username the user name, {@code null} when the URI does not carry one
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
