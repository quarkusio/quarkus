package io.quarkus.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.value.registry.ValueRegistry;

class AddressResolverTest {
    private final ValueRegistry registry = ValueRegistryImpl.builder().build();

    @Test
    void resolvesToWhereTheServerIsBound() {
        registry.register(Address.key("vertx-http"), Address.of("http", InetSocketAddress.createUnresolved("localhost", 8081)));

        assertThat(AddressResolver.resolve(registry, "quarkus://vertx-http")).hasToString("http://localhost:8081");
        assertThat(AddressResolver.resolve(registry, "quarkus://user@vertx-http/hello?x=1#top"))
                .hasToString("http://user@localhost:8081/hello?x=1#top");
    }

    @Test
    void takesTheProtocolFromTheAddress() {
        registry.register(Address.key("vertx-https"),
                Address.of("https", InetSocketAddress.createUnresolved("localhost", 8444)));

        assertThat(AddressResolver.resolve(registry, "quarkus://vertx-https/secure"))
                .hasToString("https://localhost:8444/secure");
    }

    @Test
    void wildcardBindIsReachedThroughLoopback() {
        registry.register(Address.key("vertx-http"), Address.of("http", new InetSocketAddress("0.0.0.0", 8081)));

        assertThat(AddressResolver.resolve(registry, "quarkus://vertx-http/hello"))
                .hasToString("http://localhost:8081/hello");
    }

    @Test
    void bracketsAnIpv6Host() {
        registry.register(Address.key("vertx-http"), Address.of("http", InetSocketAddress.createUnresolved("::1", 8081)));

        assertThat(AddressResolver.resolve(registry, "quarkus://vertx-http/hello"))
                .hasToString("http://[::1]:8081/hello");
    }

    @Test
    void plainUrlPassesThrough() {
        assertThat(AddressResolver.resolve("https://example.com/api")).hasToString("https://example.com/api");
        assertThat(AddressResolver.resolve(registry, "http://localhost:8080")).hasToString("http://localhost:8080");
    }

    @Test
    void referenceNeedsRunningApplication() {
        assertThatThrownBy(() -> AddressResolver.resolve("quarkus://vertx-http"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no application is running");
    }

    @Test
    void addressNotPublished() {
        assertThatThrownBy(() -> AddressResolver.resolve(registry, "quarkus://vertx-management"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vertx-management")
                .hasMessageContaining("before the server bound its sockets");
    }

    @Test
    void domainSocketOnlyAddressCannotBeReferenced() {
        registry.register(Address.key("vertx-http"),
                Address.of("http", UnixDomainSocketAddress.of(Path.of("/tmp/quarkus.sock"))));

        assertThatThrownBy(() -> AddressResolver.resolve(registry, "quarkus://vertx-http"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("/tmp/quarkus.sock");
    }

    @Test
    void referenceMustNotCarryPort() {
        assertThatThrownBy(() -> AddressResolver.resolve(registry, "quarkus://vertx-http:8080"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port");
    }

    @Test
    void referenceMustNameAnAddress() {
        assertThatThrownBy(() -> AddressResolver.resolve(registry, "quarkus:///hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not name an address");
    }

    @Test
    void blankAndInvalidValuesAreRejected() {
        assertThatThrownBy(() -> AddressResolver.resolve(registry, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> AddressResolver.resolve(registry, "http://local host"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot resolve");
    }
}
