package io.quarkus.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class AddressResolverTest {
    private final Map<String, Address> addresses = new HashMap<>();
    private final Function<String, Optional<Address>> registry = name -> Optional.ofNullable(addresses.get(name));

    @Test
    void resolvesToWhereTheServerIsBound() {
        addresses.put("vertx-http", Address.of("http", new InetSocketAddress("localhost", 8081)));

        assertThat(AddressResolver.resolve(registry, "quarkus://vertx-http")).hasToString("http://localhost:8081");
        assertThat(AddressResolver.resolve(registry, "quarkus://user@vertx-http/hello?x=1#top"))
                .hasToString("http://user@localhost:8081/hello?x=1#top");
    }

    @Test
    void takesTheProtocolFromTheAddress() {
        addresses.put("vertx-https", Address.of("https", new InetSocketAddress("localhost", 8444)));

        assertThat(AddressResolver.resolve(registry, "quarkus://vertx-https/secure"))
                .hasToString("https://localhost:8444/secure");
    }

    @Test
    void wildcardBindIsReachedThroughLoopback() {
        addresses.put("vertx-http", Address.of("http", new InetSocketAddress("0.0.0.0", 8081)));

        assertThat(AddressResolver.resolve(registry, "quarkus://vertx-http/hello"))
                .hasToString("http://localhost:8081/hello");
    }

    @Test
    void bracketsAnIpv6Host() {
        addresses.put("vertx-http", Address.of("http", new InetSocketAddress("::1", 8081)));

        assertThat(AddressResolver.resolve(registry, "quarkus://vertx-http/hello"))
                .hasToString("http://[::1]:8081/hello");
    }

    @Test
    void keepsTheEncodingOfTheReference() {
        addresses.put("vertx-http", Address.of("http", new InetSocketAddress("localhost", 8081)));

        assertThat(AddressResolver.resolve(registry, "quarkus://us%40er@vertx-http/a%20b/c%2Fd?q=x%26y&z=1#f%20g"))
                .hasToString("http://us%40er@localhost:8081/a%20b/c%2Fd?q=x%26y&z=1#f%20g");
    }

    @Test
    void plainUrlPassesThrough() {
        assertThat(AddressResolver.resolve("https://example.com/api")).hasToString("https://example.com/api");
        assertThat(AddressResolver.resolve(registry, "http://localhost:8080")).hasToString("http://localhost:8080");
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
        Path socket = Path.of("/tmp/quarkus.sock");
        addresses.put("vertx-http", Address.of("http", UnixDomainSocketAddress.of(socket)));

        assertThatThrownBy(() -> AddressResolver.resolve(registry, "quarkus://vertx-http"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(socket.toString());
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
        assertThatThrownBy(() -> AddressResolver.resolve(registry, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("may not be null");
        assertThatThrownBy(() -> AddressResolver.resolve(registry, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
        assertThatThrownBy(() -> AddressResolver.resolve(registry, "http://local host"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot resolve");
    }
}
