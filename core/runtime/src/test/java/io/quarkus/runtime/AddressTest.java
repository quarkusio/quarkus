package io.quarkus.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AddressTest {
    private static final InetSocketAddress LOCALHOST = InetSocketAddress.createUnresolved("localhost", 8081);
    private static final UnixDomainSocketAddress SOCKET = UnixDomainSocketAddress.of(Path.of("/tmp/quarkus.sock"));

    @Test
    void inetAddress() {
        Address address = Address.of("http", LOCALHOST);
        assertThat(address.protocol()).isEqualTo("http");
        assertThat(address.addresses()).containsExactly(LOCALHOST);
        assertThat(address.inet()).contains(LOCALHOST);
        assertThat(address.unix()).isEmpty();
    }

    @Test
    void domainSocketAndInetAddress() {
        Address address = Address.of("http", LOCALHOST, SOCKET);
        assertThat(address.inet()).contains(LOCALHOST);
        assertThat(address.unix()).contains(SOCKET);
        // domain sockets come first, however they were listed
        assertThat(address.addresses()).containsExactly(SOCKET, LOCALHOST);
    }

    @Test
    void socketsAreKeptInCanonicalOrder() throws Exception {
        InetSocketAddress ipv4Low = new InetSocketAddress(InetAddress.getByAddress(new byte[] { 10, 0, 0, 1 }), 9000);
        InetSocketAddress ipv4LowOtherPort = new InetSocketAddress(InetAddress.getByAddress(new byte[] { 10, 0, 0, 1 }), 80);
        InetSocketAddress ipv4High = new InetSocketAddress(
                InetAddress.getByAddress(new byte[] { (byte) 192, (byte) 168, 0, 1 }), 80);
        InetSocketAddress ipv6 = new InetSocketAddress(InetAddress.getByName("::1"), 80);
        UnixDomainSocketAddress socketA = UnixDomainSocketAddress.of(Path.of("/tmp/a.sock"));
        UnixDomainSocketAddress socketB = UnixDomainSocketAddress.of(Path.of("/tmp/b.sock"));

        Address address = Address.of("http", LOCALHOST, ipv6, ipv4High, socketB, ipv4Low, socketA, ipv4LowOtherPort);

        // domain sockets by path, IPv4 by numeric value then port, IPv6, then unresolved sockets
        assertThat(address.addresses()).containsExactly(socketA, socketB, ipv4LowOtherPort, ipv4Low, ipv4High, ipv6,
                LOCALHOST);
        assertThat(address.inet()).contains(ipv4LowOtherPort);
        assertThat(address.unix()).contains(socketA);
    }

    @Test
    void requiresProtocolAndSocket() {
        assertThatThrownBy(() -> Address.of(" ", LOCALHOST))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("protocol");
        assertThatThrownBy(() -> Address.of("http"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("socket");
    }

    @Test
    void equalityIsByProtocolAndSockets() {
        assertThat(Address.of("http", LOCALHOST)).isEqualTo(Address.of("http", LOCALHOST));
        assertThat(Address.of("http", LOCALHOST)).isNotEqualTo(Address.of("https", LOCALHOST));
        assertThat(Address.of("http", LOCALHOST)).isNotEqualTo(Address.of("http", LOCALHOST, SOCKET));
        assertThat(Address.of("http", LOCALHOST, SOCKET)).isEqualTo(Address.of("http", SOCKET, LOCALHOST));
    }
}
