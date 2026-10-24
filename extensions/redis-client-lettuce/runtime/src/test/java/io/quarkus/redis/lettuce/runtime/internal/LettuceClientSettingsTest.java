package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.lettuce.core.CredentialsProvider;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.SslVerifyMode;
import io.lettuce.core.StaticCredentialsProvider;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.resource.Delay;
import io.lettuce.core.resource.NettyCustomizer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.quarkus.redis.lettuce.runtime.internal.LettuceClientSettings.UserInfo;
import io.quarkus.redis.runtime.client.config.RedisClientConfig;
import io.quarkus.redis.runtime.client.config.RedisConfig;
import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.vertx.core.net.NetClientOptions;
import io.vertx.redis.client.RedisReplicas;
import io.vertx.redis.client.RedisRole;

/**
 * Unit tests for the URI credential parsing, the credential precedence, the peer verification mapping and the
 * client option mappings of {@link LettuceClientSettings}, all of which must match the Vert.x Redis client.
 */
class LettuceClientSettingsTest {

    @Test
    void parsesUserInfoLikeVertx() {
        assertThat(UserInfo.parse(URI.create("redis://localhost:6379")))
                .isEqualTo(UserInfo.NONE);
        assertThat(UserInfo.parse(URI.create("redis://user:pass@localhost:6379")))
                .isEqualTo(new UserInfo("user", "pass"));
        assertThat(UserInfo.parse(URI.create("redis://:pass@localhost:6379")))
                .isEqualTo(new UserInfo(null, "pass"));
        // a lone user info is the user name (Vert.x), not the password (Lettuce)
        assertThat(UserInfo.parse(URI.create("redis://user@localhost:6379")))
                .isEqualTo(new UserInfo("user", null));
        // an empty password counts as absent, so the password property applies
        assertThat(UserInfo.parse(URI.create("redis://user:@localhost:6379")))
                .isEqualTo(new UserInfo("user", null));
        // parts are URL-decoded after splitting, so an encoded colon or at-sign is not a separator
        assertThat(UserInfo.parse(URI.create("redis://us%40er:p%3Ass@localhost:6379")))
                .isEqualTo(new UserInfo("us@er", "p:ss"));
        // the Vert.x query parameters are fallbacks, split first and then decoded like the user info: an encoded
        // ampersand is not a separator (Vert.x decodes before splitting and reads `p`), and a plus sign is a space
        // (Vert.x keeps the plus sign in the query, but not in the user info)
        assertThat(UserInfo.parse(URI.create("redis://localhost:6379?user=u&password=p%26q")))
                .isEqualTo(new UserInfo("u", "p&q"));
        assertThat(UserInfo.parse(URI.create("redis://localhost:6379?password=p+q")))
                .isEqualTo(new UserInfo(null, "p q"));
        assertThat(UserInfo.parse(URI.create("redis://a:b@localhost:6379?user=u&password=p")))
                .isEqualTo(new UserInfo("a", "b"));
    }

    @Test
    void uriPasswordWinsOverProperty() {
        RedisCredentials fromUri = resolve(new UserInfo("user", "uri-pass"), "property-pass");
        assertThat(fromUri.getUsername()).isEqualTo("user");
        assertThat(fromUri.getPassword()).containsExactly("uri-pass".toCharArray());

        RedisCredentials fromProperty = resolve(new UserInfo("user", null), "property-pass");
        assertThat(fromProperty.getUsername()).isEqualTo("user");
        assertThat(fromProperty.getPassword()).containsExactly("property-pass".toCharArray());

        RedisCredentials defaultUser = resolve(UserInfo.NONE, "property-pass");
        assertThat(defaultUser.hasUsername()).isFalse();
        assertThat(defaultUser.getPassword()).containsExactly("property-pass".toCharArray());

        RedisCredentials none = resolve(new UserInfo("user", null), null);
        assertThat(none.getUsername()).isEqualTo("user");
        assertThat(none.hasPassword()).isFalse();
    }

    @Test
    void uriUsernameWinsOverProperty() {
        RedisCredentials fromUri = resolve(new UserInfo("uri-user", "pass"), "property-user", null);
        assertThat(fromUri.getUsername()).isEqualTo("uri-user");
        assertThat(fromUri.getPassword()).containsExactly("pass".toCharArray());

        RedisCredentials fromProperty = resolve(new UserInfo(null, "pass"), "property-user", null);
        assertThat(fromProperty.getUsername()).isEqualTo("property-user");
        assertThat(fromProperty.getPassword()).containsExactly("pass".toCharArray());

        RedisCredentials bothProperties = resolve(UserInfo.NONE, "property-user", "property-pass");
        assertThat(bothProperties.getUsername()).isEqualTo("property-user");
        assertThat(bothProperties.getPassword()).containsExactly("property-pass".toCharArray());

        RedisCredentials none = resolve(UserInfo.NONE, null, "property-pass");
        assertThat(none.hasUsername()).isFalse();
    }

    private static RedisCredentials resolve(UserInfo userInfo, String password) {
        return resolve(userInfo, null, password);
    }

    private static RedisCredentials resolve(UserInfo userInfo, String username, String password) {
        return ((StaticCredentialsProvider) LettuceClientSettings.credentials(userInfo, username, password))
                .resolveCredentialsNow();
    }

    @Test
    void mapsPeerVerificationIndependentlyOfTrustAll() {
        NetClientOptions net = new NetClientOptions();
        // Vert.x default: no hostname verification, certificate chain verified
        assertThat(LettuceClientSettings.verifyMode(net)).isEqualTo(SslVerifyMode.CA);

        net.setHostnameVerificationAlgorithm("HTTPS");
        assertThat(LettuceClientSettings.verifyMode(net)).isEqualTo(SslVerifyMode.FULL);

        // trust-all only affects the chain validation (through the trust manager), never the hostname verification
        net.setTrustAll(true);
        assertThat(LettuceClientSettings.verifyMode(net)).isEqualTo(SslVerifyMode.FULL);

        net.setHostnameVerificationAlgorithm("");
        assertThat(LettuceClientSettings.verifyMode(net)).isEqualTo(SslVerifyMode.CA);

        // the TLS registry passes `hostname-verification-algorithm=NONE` on verbatim, without the translation to an
        // empty algorithm that the quarkus.redis.tls.* path performs
        net.setHostnameVerificationAlgorithm("NONE");
        assertThat(LettuceClientSettings.verifyMode(net)).isEqualTo(SslVerifyMode.CA);
        net.setHostnameVerificationAlgorithm("none");
        assertThat(LettuceClientSettings.verifyMode(net)).isEqualTo(SslVerifyMode.CA);
    }

    @Test
    void mapsReplicasOntoReadFrom() {
        assertThat(LettuceClientSettings.readFrom(Optional.empty())).isEqualTo(ReadFrom.UPSTREAM);
        assertThat(LettuceClientSettings.readFrom(Optional.of(RedisReplicas.NEVER))).isEqualTo(ReadFrom.UPSTREAM);
        assertThat(LettuceClientSettings.readFrom(Optional.of(RedisReplicas.SHARE))).isEqualTo(ReadFrom.ANY);
        // like the Vert.x client, ALWAYS falls back to the upstream node of a shard without a usable replica
        assertThat(LettuceClientSettings.readFrom(Optional.of(RedisReplicas.ALWAYS))).isEqualTo(ReadFrom.REPLICA_PREFERRED);
    }

    @Test
    void refreshesTheTopologyPeriodicallyAndAdaptively() {
        ClusterTopologyRefreshOptions options = LettuceClientSettings.topologyRefreshOptions(Duration.ofSeconds(2));
        assertThat(options.isPeriodicRefreshEnabled()).isTrue();
        assertThat(options.getRefreshPeriod()).isEqualTo(Duration.ofSeconds(2));
        assertThat(options.getAdaptiveRefreshTriggers()).containsExactlyInAnyOrder(RefreshTrigger.values());

        // a non-positive TTL disables the topology cache of the Vert.x client; Lettuce needs a positive period, so
        // only the adaptive refresh is left
        ClusterTopologyRefreshOptions uncached = LettuceClientSettings.topologyRefreshOptions(Duration.ZERO);
        assertThat(uncached.isPeriodicRefreshEnabled()).isFalse();
        assertThat(uncached.getAdaptiveRefreshTriggers()).containsExactlyInAnyOrder(RefreshTrigger.values());
    }

    @Test
    void mapsTheSentinelRoleOntoReadFrom() {
        // role=replica reads from the replicas whatever replicas says; otherwise replicas decides as for a cluster
        assertThat(LettuceClientSettings.readFrom(Optional.of(RedisRole.REPLICA), Optional.empty()))
                .isEqualTo(ReadFrom.REPLICA_PREFERRED);
        assertThat(LettuceClientSettings.readFrom(Optional.of(RedisRole.REPLICA), Optional.of(RedisReplicas.NEVER)))
                .isEqualTo(ReadFrom.REPLICA_PREFERRED);
        assertThat(LettuceClientSettings.readFrom(Optional.empty(), Optional.empty())).isEqualTo(ReadFrom.UPSTREAM);
        assertThat(LettuceClientSettings.readFrom(Optional.of(RedisRole.MASTER), Optional.of(RedisReplicas.SHARE)))
                .isEqualTo(ReadFrom.ANY);
    }

    @Test
    void buildsTheSentinelUriFromTheHosts() {
        RedisURI first = RedisURI.create(URI.create("rediss://s1:26379/2?timeout=7s&clientName=app"));
        first.setCredentialsProvider(new StaticCredentialsProvider("user", "secret".toCharArray()));
        first.setVerifyPeer(SslVerifyMode.CA);
        RedisURI second = RedisURI.create(URI.create("rediss://s2:26380?timeout=7s&clientName=app"));
        second.setCredentialsProvider(first.getCredentialsProviderAsync());
        second.setVerifyPeer(SslVerifyMode.CA);

        RedisURI master = LettuceClientSettings.sentinelUri(java.util.List.of(first, second), "mymaster");

        assertThat(master.getSentinelMasterId()).isEqualTo("mymaster");
        assertThat(master.getDatabase()).isEqualTo(2);
        assertThat(master.isSsl()).isTrue();
        assertThat(master.getVerifyMode()).isEqualTo(SslVerifyMode.CA);
        assertThat(credentialsOf(master).getUsername())
                .isEqualTo("user");
        // the timeout bounds the connection and the SENTINEL queries, the client name is set on every connection
        assertThat(master.getTimeout()).isEqualTo(Duration.ofSeconds(7));
        assertThat(master.getClientName()).isEqualTo("app");
        // every sentinel carries the host's credentials, TLS settings, timeout and client name: Lettuce does not copy
        // them from the master; a sentinel has no databases
        assertThat(master.getSentinels()).hasSize(2).allSatisfy(sentinel -> {
            assertThat(sentinel.isSsl()).isTrue();
            assertThat(sentinel.getVerifyMode()).isEqualTo(SslVerifyMode.CA);
            assertThat(credentialsOf(sentinel)
                    .getPassword()).containsExactly("secret".toCharArray());
            assertThat(sentinel.getTimeout()).isEqualTo(Duration.ofSeconds(7));
            assertThat(sentinel.getClientName()).isEqualTo("app");
            assertThat(sentinel.getDatabase()).isZero();
        });
        assertThat(master.getSentinels().get(0).getHost()).isEqualTo("s1");
        assertThat(master.getSentinels().get(0).getPort()).isEqualTo(26379);
        assertThat(master.getSentinels().get(1).getHost()).isEqualTo("s2");
        assertThat(master.getSentinels().get(1).getPort()).isEqualTo(26380);
    }

    @Test
    void leavesTheSentinelUriUnnamedWithoutAClientName() {
        // no client name on the hosts, none on the master and the sentinels (Lettuce rejects a null name)
        RedisURI master = LettuceClientSettings.sentinelUri(List.of(RedisURI.create(URI.create("redis://s1:26379"))),
                "mymaster");

        assertThat(master.getClientName()).isNull();
        assertThat(master.getSentinels().get(0).getClientName()).isNull();
    }

    @Test
    void mapsTheProtocolNegotiationOntoTheProtocolVersion() {
        // negotiation on without a preference, or RESP3 preferred: Lettuce negotiates the newest version the
        // server supports and falls back to RESP2, as the Vert.x client does
        assertThat(LettuceClientSettings.protocolVersion(config(Map.of()))).isEmpty();
        assertThat(LettuceClientSettings.protocolVersion(config(Map.of(
                "quarkus.redis.preferred-protocol-version", "resp3")))).isEmpty();
        // RESP2 preferred, or negotiation off: RESP2 without HELLO
        assertThat(LettuceClientSettings.protocolVersion(config(Map.of(
                "quarkus.redis.preferred-protocol-version", "resp2")))).contains(ProtocolVersion.RESP2);
        assertThat(LettuceClientSettings.protocolVersion(config(Map.of(
                "quarkus.redis.protocol-negotiation", "false")))).contains(ProtocolVersion.RESP2);
        assertThat(LettuceClientSettings.protocolVersion(config(Map.of(
                "quarkus.redis.protocol-negotiation", "false",
                "quarkus.redis.preferred-protocol-version", "resp3")))).contains(ProtocolVersion.RESP2);
    }

    @Test
    void namesTheConnectionLikeVertx() {
        URI host = URI.create("redis://localhost:6379");
        // client-name alone is not applied, as with the Vert.x client
        assertThat(clientName(config(Map.of("quarkus.redis.client-name", "my-app")), host)).isNull();
        assertThat(clientName(config(Map.of()), host)).isNull();
        assertThat(clientName(config(Map.of(
                "quarkus.redis.configure-client-name", "true",
                "quarkus.redis.client-name", "my-app")), host)).isEqualTo("my-app");
        // without client-name, the Quarkus client name
        assertThat(clientName(config(Map.of("quarkus.redis.configure-client-name", "true")), host))
                .isEqualTo(RedisConfig.DEFAULT_CLIENT_NAME);
        // the client query parameter of the URI always applies and wins over the properties
        URI named = URI.create("redis://localhost:6379?client=from-uri");
        assertThat(clientName(config(Map.of()), named)).isEqualTo("from-uri");
        assertThat(clientName(config(Map.of(
                "quarkus.redis.configure-client-name", "true",
                "quarkus.redis.client-name", "my-app")), named)).isEqualTo("from-uri");
    }

    private static String clientName(RedisClientConfig config, URI host) {
        return LettuceClientSettings.clientName(RedisConfig.DEFAULT_CLIENT_NAME, config, host);
    }

    @Test
    void mapsTheTcpPropertiesOntoTheSocketOptions() {
        assertThat(LettuceClientSettings.socketOptions(config(Map.of()).tcp())).isEmpty();

        SocketOptions socket = LettuceClientSettings.socketOptions(config(Map.of(
                "quarkus.redis.tcp.connection-timeout", "3s",
                "quarkus.redis.tcp.keep-alive", "false",
                "quarkus.redis.tcp.no-delay", "false")).tcp()).orElseThrow();
        assertThat(socket.getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(socket.isKeepAlive()).isFalse();
        assertThat(socket.isTcpNoDelay()).isFalse();

        // only the set properties are applied: the Lettuce defaults hold for the others, not the Vert.x ones
        socket = LettuceClientSettings.socketOptions(config(Map.of("quarkus.redis.tcp.connection-timeout", "3s")).tcp())
                .orElseThrow();
        assertThat(socket.isKeepAlive()).isTrue();
        assertThat(socket.isTcpNoDelay()).isTrue();

        // a connect timeout of zero (no timeout, on the Vert.x client) is not applied: Lettuce requires a positive one
        assertThat(LettuceClientSettings.socketOptions(config(Map.of("quarkus.redis.tcp.connection-timeout", "0")).tcp()))
                .isEmpty();
        socket = LettuceClientSettings.socketOptions(config(Map.of(
                "quarkus.redis.tcp.connection-timeout", "0",
                "quarkus.redis.tcp.keep-alive", "false")).tcp()).orElseThrow();
        assertThat(socket.getConnectTimeout()).isEqualTo(SocketOptions.DEFAULT_CONNECT_TIMEOUT_DURATION);
        assertThat(socket.isKeepAlive()).isFalse();
    }

    @Test
    void mapsTheReconnectIntervalOntoAConstantDelay() {
        // neither property set: Lettuce keeps its exponential backoff, whatever the mapped defaults say
        assertThat(reconnectDelay(config(Map.of()))).isEmpty();

        // set, even to the default interval: a constant delay
        Delay delay = reconnectDelay(config(Map.of("quarkus.redis.reconnect-interval", "1s"))).orElseThrow();
        assertThat(delay.createDelay(1)).isEqualTo(Duration.ofSeconds(1));
        delay = reconnectDelay(config(Map.of("quarkus.redis.reconnect-interval", "2s"))).orElseThrow();
        assertThat(delay.createDelay(1)).isEqualTo(Duration.ofSeconds(2));
        assertThat(delay.createDelay(10)).isEqualTo(Duration.ofSeconds(2));

        // tcp.reconnect-interval overrides the client-level property, as with the Vert.x client, even when set to
        // the default interval
        delay = reconnectDelay(config(Map.of(
                "quarkus.redis.reconnect-interval", "2s",
                "quarkus.redis.tcp.reconnect-interval", "500ms"))).orElseThrow();
        assertThat(delay.createDelay(1)).isEqualTo(Duration.ofMillis(500));
        delay = reconnectDelay(config(Map.of(
                "quarkus.redis.reconnect-interval", "2s",
                "quarkus.redis.tcp.reconnect-interval", "1s"))).orElseThrow();
        assertThat(delay.createDelay(1)).isEqualTo(Duration.ofSeconds(1));
        delay = reconnectDelay(config(Map.of("quarkus.redis.tcp.reconnect-interval", "3s"))).orElseThrow();
        assertThat(delay.createDelay(1)).isEqualTo(Duration.ofSeconds(3));
    }

    private static Optional<Delay> reconnectDelay(RedisClientConfig config) {
        return LettuceClientSettings.reconnectDelay(RedisConfig.DEFAULT_CLIENT_NAME, config);
    }

    @Test
    void mapsTheSocketPropertiesOntoChannelOptions() {
        assertThat(LettuceClientSettings.nettyCustomizer(config(Map.of()).tcp())).isEmpty();

        NettyCustomizer customizer = LettuceClientSettings.nettyCustomizer(config(Map.of(
                "quarkus.redis.tcp.receive-buffer-size", "65536",
                "quarkus.redis.tcp.send-buffer-size", "32768",
                "quarkus.redis.tcp.so-linger", "5s",
                "quarkus.redis.tcp.traffic-class", "16",
                "quarkus.redis.tcp.reuse-address", "true",
                "quarkus.redis.tcp.local-address", "127.0.0.1")).tcp()).orElseThrow();
        Bootstrap bootstrap = new Bootstrap();
        customizer.afterBootstrapInitialized(bootstrap);
        assertThat(bootstrap.config().options())
                .containsEntry(ChannelOption.SO_RCVBUF, 65536)
                .containsEntry(ChannelOption.SO_SNDBUF, 32768)
                // SO_LINGER is in seconds
                .containsEntry(ChannelOption.SO_LINGER, 5)
                .containsEntry(ChannelOption.IP_TOS, 16)
                .containsEntry(ChannelOption.SO_REUSEADDR, true);
        assertThat(bootstrap.config().localAddress()).isEqualTo(new InetSocketAddress("127.0.0.1", 0));

        // a single property is enough, and the local address alone too
        customizer = LettuceClientSettings.nettyCustomizer(config(Map.of("quarkus.redis.tcp.local-address", "127.0.0.1"))
                .tcp()).orElseThrow();
        bootstrap = new Bootstrap();
        customizer.afterBootstrapInitialized(bootstrap);
        assertThat(bootstrap.config().options()).isEmpty();
        assertThat(bootstrap.config().localAddress()).isEqualTo(new InetSocketAddress("127.0.0.1", 0));
    }

    @Test
    void appliesTheClientPropertiesToTheOptionsAndTheUri() {
        List<URI> hosts = List.of(URI.create("redis://localhost:6379"));
        LettuceClientSettings settings = LettuceClientSettings.create(RedisConfig.DEFAULT_CLIENT_NAME, config(Map.of(
                "quarkus.redis.max-waiting-handlers", "100",
                "quarkus.redis.preferred-protocol-version", "resp2",
                "quarkus.redis.configure-client-name", "true",
                "quarkus.redis.client-name", "my-app",
                "quarkus.redis.tcp.connection-timeout", "3s",
                "quarkus.redis.tcp.receive-buffer-size", "65536",
                "quarkus.redis.reconnect-interval", "2s")), hosts, null, NO_TLS);
        assertThat(settings.clientOptions().getRequestQueueSize()).isEqualTo(100);
        assertThat(settings.clientOptions().getConfiguredProtocolVersion()).isEqualTo(ProtocolVersion.RESP2);
        assertThat(settings.clientOptions().getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(settings.redisUri().getClientName()).isEqualTo("my-app");
        assertThat(settings.reconnectDelay()).isPresent();
        assertThat(settings.nettyCustomizer()).isPresent();

        // the defaults: no queue bound (the Vert.x default of 2048 would bound the single shared connection), the
        // Lettuce protocol negotiation, no name
        settings = LettuceClientSettings.create(RedisConfig.DEFAULT_CLIENT_NAME, config(Map.of()), hosts, null, NO_TLS);
        assertThat(settings.clientOptions().getRequestQueueSize()).isEqualTo(Integer.MAX_VALUE);
        assertThat(settings.clientOptions().getConfiguredProtocolVersion()).isNull();
        assertThat(settings.redisUri().getClientName()).isNull();
        assertThat(settings.reconnectDelay()).isEmpty();
        assertThat(settings.nettyCustomizer()).isEmpty();
    }

    /** A TLS registry without any configuration: the TLS settings are not under test here. */
    private static final TlsConfigurationRegistry NO_TLS = new TlsConfigurationRegistry() {
        @Override
        public Optional<TlsConfiguration> get(String name) {
            return Optional.empty();
        }

        @Override
        public Optional<TlsConfiguration> getDefault() {
            return Optional.empty();
        }

        @Override
        public void register(String name, TlsConfiguration configuration) {
        }
    };

    /**
     * The configuration of the default client, mapped from the given {@code quarkus.redis.*} properties the way the
     * application configuration is (with the Quarkus duration format, {@code 10s}, which SmallRye Config alone does
     * not read). The configuration is also registered as the one of the class loader, replacing the previous one:
     * the settings ask it which properties are set (see {@code LettuceClientSettings.isSet}).
     */
    private static RedisClientConfig config(Map<String, String> properties) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withConverter(Duration.class, 100, new DurationConverter())
                .withMapping(RedisConfig.class)
                .withSources(new PropertiesConfigSource(properties, "test", 100))
                .build();
        releaseConfig();
        ConfigProviderResolver.instance().registerConfig(config, Thread.currentThread().getContextClassLoader());
        return config.getConfigMapping(RedisConfig.class).clients().get(RedisConfig.DEFAULT_CLIENT_NAME);
    }

    /** Releases the configuration registered for the class loader, a default one included. */
    @AfterAll
    static void releaseConfig() {
        ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        resolver.releaseConfig(resolver.getConfig(Thread.currentThread().getContextClassLoader()));
    }

    private static RedisCredentials credentialsOf(RedisURI uri) {
        return ((CredentialsProvider.ImmediateCredentialsProvider) uri.getCredentialsProviderAsync()).resolveCredentialsNow();
    }
}
