package io.quarkus.devservices.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class DevServicesHostUtilTest {

    @Test
    public void ipv4() {
        assertThat(DevServicesHostUtil.formatHostAndPort("127.0.0.1", 5432)).isEqualTo("127.0.0.1:5432");
    }

    @Test
    public void hostname() {
        assertThat(DevServicesHostUtil.formatHostAndPort("myhost", 5432)).isEqualTo("myhost:5432");
    }

    @Test
    public void aliasPassesThroughUnchanged() {
        assertThat(DevServicesHostUtil.formatHostAndPort("mariadb-abc12", 3306)).isEqualTo("mariadb-abc12:3306");
    }

    @Test
    public void ipv6() {
        assertThat(DevServicesHostUtil.formatHostAndPort("fd00:d0ca:1::1", 32769))
                .isEqualTo("[fd00:d0ca:1::1]:32769");
    }

    @Test
    public void alreadyBracketed() {
        assertThat(DevServicesHostUtil.formatHostAndPort("[::1]", 8080)).isEqualTo("[::1]:8080");
    }

    @Test
    public void prefixedKafkaListener() {
        assertThat(DevServicesHostUtil.formatPrefixedAuthority("PLAINTEXT", "fd00:d0ca:1::1", 9092))
                .isEqualTo("PLAINTEXT://[fd00:d0ca:1::1]:9092");
    }

    @Test
    public void nullHostRejected() {
        assertThatThrownBy(() -> DevServicesHostUtil.formatHostForUriAuthority(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("host");
    }

    @Test
    public void isIPv6LiteralDetection() {
        assertThat(DevServicesHostUtil.isIPv6Literal("fd00::1")).isTrue();
        assertThat(DevServicesHostUtil.isIPv6Literal("::1")).isTrue();
        assertThat(DevServicesHostUtil.isIPv6Literal("[fd00::1]")).isTrue();
        assertThat(DevServicesHostUtil.isIPv6Literal("fd00:d0ca:1::1")).isTrue();
        assertThat(DevServicesHostUtil.isIPv6Literal("172.17.0.1")).isFalse();
        assertThat(DevServicesHostUtil.isIPv6Literal("mongo-abc")).isFalse();
    }

    @Test
    public void resolvePublishedPortHostNonIPv6Unchanged() {
        assertThat(DevServicesHostUtil.resolvePublishedPortHost("id", "172.17.0.1")).isEqualTo("172.17.0.1");
        assertThat(DevServicesHostUtil.resolvePublishedPortHost("id", "postgres-abc")).isEqualTo("postgres-abc");
        assertThat(DevServicesHostUtil.resolvePublishedPortHost("id", "0.0.0.0")).isEqualTo("localhost");
    }

    @Test
    public void publishedPortHostSharedNetworkUsesAlias() {
        assertThat(DevServicesHostUtil.publishedPortHost("id", true, "mongo-abc12", "fd00:d0ca:1::1"))
                .isEqualTo("mongo-abc12");
    }

    @Test
    public void publishedPortHostTwoArgDelegatesToResolve() {
        assertThat(DevServicesHostUtil.publishedPortHost("id", "172.17.0.1")).isEqualTo("172.17.0.1");
    }

    @Test
    public void resolvePublishedPortHostNeverReturnsIPv6() {
        assertThat(DevServicesHostUtil.resolvePublishedPortHost("quarkus-nonexistent-container-id",
                "fd00:d0ca:1::1")).isEqualTo("localhost");
        assertThat(DevServicesHostUtil.formatResolvedHostAndPort("quarkus-nonexistent-container-id",
                "fd00:d0ca:1::1", 32783)).isEqualTo("localhost:32783");
    }

    @Test
    public void resolvePublishedPortHostWithoutContainerIdFallsBackToLocalhost() {
        assertThat(DevServicesHostUtil.resolvePublishedPortHost(null, "fd00:d0ca:1::1")).isEqualTo("localhost");
        assertThat(DevServicesHostUtil.resolvePublishedPortHost("", "fd00:d0ca:1::1")).isEqualTo("localhost");
    }
}
