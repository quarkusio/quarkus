package io.quarkus.redis.lettuce.runtime.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.lettuce.core.CredentialsProvider;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslVerifyMode;
import io.lettuce.core.StaticCredentialsProvider;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger;
import io.quarkus.redis.lettuce.runtime.internal.LettuceClientSettings.UserInfo;
import io.vertx.core.net.NetClientOptions;
import io.vertx.redis.client.RedisReplicas;
import io.vertx.redis.client.RedisRole;

/**
 * Unit tests for the URI credential parsing, the password precedence and the peer verification mapping of
 * {@link LettuceClientSettings}, all of which must match the Vert.x Redis client.
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

    private static RedisCredentials resolve(UserInfo userInfo, String password) {
        return ((StaticCredentialsProvider) LettuceClientSettings.credentials(userInfo, password)).resolveCredentialsNow();
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

    private static RedisCredentials credentialsOf(RedisURI uri) {
        return ((CredentialsProvider.ImmediateCredentialsProvider) uri.getCredentialsProviderAsync()).resolveCredentialsNow();
    }
}
